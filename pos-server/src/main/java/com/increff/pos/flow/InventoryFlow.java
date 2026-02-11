package com.increff.pos.flow;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.exception.ApiException;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryFlow {

    private static final int INVENTORY_MAX = 1000;

    @Autowired
    private ProductApi productApi;

    @Autowired
    private InventoryApi inventoryApi;

    public Pair<ProductPojo, InventoryPojo> getByBarcode(String barcode) throws ApiException {
        ProductPojo product = productApi.getProductByBarcode(barcode);
        InventoryPojo inventory = inventoryApi.getByProductId(product.getId());
        return Pair.of(product, inventory);
    }

    public Pair<ProductPojo, InventoryPojo> updateInventory(InventoryPojo inventoryToUpdate) throws ApiException {
        InventoryPojo updatedInventory = inventoryApi.updateInventory(inventoryToUpdate);
        ProductPojo product = productApi.getById(inventoryToUpdate.getProductId());
        return Pair.of(product, updatedInventory);
    }

    // ---------------- BULK ----------------

    public List<String[]> bulkUpdateInventory(List<InventoryPojo> incomingInventoryRows) {
        List<String> barcodeByRowIndexList = extractInventoryBarcodesByRowIndex(incomingInventoryRows);
        List<String[]> resultRows = initializeBulkInventoryResultRows(barcodeByRowIndexList);

        if (incomingInventoryRows == null || incomingInventoryRows.isEmpty()) {
            return resultRows;
        }

        AggregatedInventoryInput aggregatedInput = aggregateInventoryDeltasByBarcode(incomingInventoryRows);
        if (aggregatedInput.totalDeltaByBarcode().isEmpty()) {
            enforceBarcodeColumn(resultRows, barcodeByRowIndexList);
            return resultRows;
        }

        Map<String, ProductPojo> barcodeProductMap = fetchProductsByBarcode(aggregatedInput.totalDeltaByBarcode().keySet());
        Map<String, InventoryPojo> productIdInventoryMap = fetchInventoryByProductId(barcodeProductMap.values());

        List<InventoryPojo> inventoriesToSave = new ArrayList<>();
        applyAggregatedInventoryUpdates(
                aggregatedInput,
                barcodeProductMap,
                productIdInventoryMap,
                inventoriesToSave,
                resultRows
        );

        if (!inventoriesToSave.isEmpty()) {
            inventoryApi.saveAll(inventoriesToSave);
        }

        enforceBarcodeColumn(resultRows, barcodeByRowIndexList);
        return resultRows;
    }

    // -------------------- private helpers --------------------

    private AggregatedInventoryInput aggregateInventoryDeltasByBarcode(List<InventoryPojo> incomingInventoryRows) {
        Map<String, Integer> barcodeTotalDeltaMap = new LinkedHashMap<>();
        Map<String, List<Integer>> barcodeRowIndicesMap = new HashMap<>();

        for (int rowIndex = 0; rowIndex < incomingInventoryRows.size(); rowIndex++) {
            InventoryPojo incomingRow = incomingInventoryRows.get(rowIndex);
            if (incomingRow == null) continue;

            String barcode = incomingRow.getProductId();   // barcode carrier
            Integer delta = incomingRow.getQuantity();     // delta carrier

            // DTO validates numeric type etc; allow negative
            barcodeTotalDeltaMap.merge(barcode, delta, Integer::sum);
            barcodeRowIndicesMap.computeIfAbsent(barcode, ignored -> new ArrayList<>()).add(rowIndex);
        }

        return new AggregatedInventoryInput(barcodeTotalDeltaMap, barcodeRowIndicesMap);
    }

    private Map<String, ProductPojo> fetchProductsByBarcode(Set<String> barcodes) {
        if (barcodes == null || barcodes.isEmpty()) return Map.of();

        return productApi.findByBarcodes(new ArrayList<>(barcodes))
                .stream()
                .collect(Collectors.toMap(ProductPojo::getBarcode, p -> p));
    }

    private Map<String, InventoryPojo> fetchInventoryByProductId(Collection<ProductPojo> products) {
        if (products == null || products.isEmpty()) return Map.of();

        List<String> productIdList = products.stream().map(ProductPojo::getId).toList();
        return inventoryApi.getByProductIds(productIdList)
                .stream()
                .collect(Collectors.toMap(InventoryPojo::getProductId, i -> i));
    }

    private void applyAggregatedInventoryUpdates(
            AggregatedInventoryInput aggregatedInput,
            Map<String, ProductPojo> barcodeProductMap,
            Map<String, InventoryPojo> productIdInventoryMap,
            List<InventoryPojo> inventoriesToSave,
            List<String[]> resultRows
    ) {
        for (Map.Entry<String, Integer> entry : aggregatedInput.totalDeltaByBarcode().entrySet()) {
            String barcode = entry.getKey();
            int totalDelta = entry.getValue();

            List<Integer> rowIndexList = aggregatedInput.rowIndicesByBarcode().getOrDefault(barcode, List.of());
            ProductPojo product = barcodeProductMap.get(barcode);

            if (product == null) {
                markBulkInventoryRowsError(resultRows, rowIndexList, "Product not found");
                continue;
            }

            InventoryPojo inventory = productIdInventoryMap.get(product.getId());
            if (inventory == null) {
                markBulkInventoryRowsError(resultRows, rowIndexList, "Inventory not found for barcode: " + barcode);
                continue;
            }

            int currentQuantity = inventory.getQuantity() == null ? 0 : inventory.getQuantity();
            int computedNextQuantity = currentQuantity + totalDelta;

            if (computedNextQuantity < 0) {
                inventory.setQuantity(0);
                inventoriesToSave.add(inventory);
                markBulkInventoryRowsError(
                        resultRows,
                        rowIndexList,
                        "Inventory cannot go below 0 (clamped to 0)"
                );
                continue;
            }

            if (computedNextQuantity > INVENTORY_MAX) {
                inventory.setQuantity(INVENTORY_MAX);
                inventoriesToSave.add(inventory);
                markBulkInventoryRowsError(
                        resultRows,
                        rowIndexList,
                        "Inventory cannot exceed " + INVENTORY_MAX + " (clamped to " + INVENTORY_MAX + ")"
                );
                continue;
            }

            inventory.setQuantity(computedNextQuantity);
            inventoriesToSave.add(inventory);
            markBulkInventoryRowsSuccess(resultRows, rowIndexList);
        }
    }

    private void markBulkInventoryRowsError(List<String[]> resultRows, List<Integer> rowIndexList, String message) {
        for (Integer rowIndex : rowIndexList) {
            resultRows.get(rowIndex)[1] = "ERROR";
            resultRows.get(rowIndex)[2] = message;
        }
    }

    private void markBulkInventoryRowsSuccess(List<String[]> resultRows, List<Integer> rowIndexList) {
        for (Integer rowIndex : rowIndexList) {
            resultRows.get(rowIndex)[1] = "SUCCESS";
            resultRows.get(rowIndex)[2] = "";
        }
    }

    // -------------------- barcode-preserving bulk result helpers --------------------

    private List<String> extractInventoryBarcodesByRowIndex(List<InventoryPojo> incomingInventoryRows) {
        if (incomingInventoryRows == null) return new ArrayList<>();
        List<String> barcodeByRowIndexList = new ArrayList<>(incomingInventoryRows.size());
        for (InventoryPojo r : incomingInventoryRows) {
            barcodeByRowIndexList.add(r == null ? "" : safeString(r.getProductId()));
        }
        return barcodeByRowIndexList;
    }

    private List<String[]> initializeBulkInventoryResultRows(List<String> barcodeByRowIndexList) {
        if (barcodeByRowIndexList == null) return new ArrayList<>();
        List<String[]> resultRows = new ArrayList<>(barcodeByRowIndexList.size());
        for (String barcode : barcodeByRowIndexList) {
            resultRows.add(new String[]{safeString(barcode), "ERROR", ""});
        }
        return resultRows;
    }

    private void enforceBarcodeColumn(List<String[]> resultRows, List<String> barcodeByRowIndexList) {
        if (resultRows == null || barcodeByRowIndexList == null) return;

        int n = Math.min(resultRows.size(), barcodeByRowIndexList.size());
        for (int i = 0; i < n; i++) {
            String[] row = resultRows.get(i);
            if (row == null || row.length == 0) continue;
            row[0] = safeString(barcodeByRowIndexList.get(i));
        }
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private record AggregatedInventoryInput(
            Map<String, Integer> totalDeltaByBarcode,
            Map<String, List<Integer>> rowIndicesByBarcode
    ) {}
}
