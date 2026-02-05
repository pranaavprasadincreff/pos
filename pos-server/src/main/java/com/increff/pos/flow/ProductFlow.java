package com.increff.pos.flow;

import com.increff.pos.api.ClientApi;
import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.InventoryUpdatePojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.db.ProductUpdatePojo;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.PageForm;
import com.increff.pos.model.form.ProductFilterForm;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProductFlow {
    private static final int INVENTORY_MAX = 1000;
    private static final int CLIENT_MATCH_LIMIT = 1000;

    @Autowired
    private ProductApi productApi;

    @Autowired
    private InventoryApi inventoryApi;

    @Autowired
    private ClientApi clientApi;

    @Transactional(rollbackFor = ApiException.class)
    public Pair<ProductPojo, InventoryPojo> addProduct(ProductPojo productToCreate) throws ApiException {
        ensureClientExistsByEmail(productToCreate.getClientEmail());

        ProductPojo createdProduct = productApi.addProduct(productToCreate);
        InventoryPojo inventory = getOrCreateInventoryForProductId(createdProduct.getId());

        return Pair.of(createdProduct, inventory);
    }

    public Pair<ProductPojo, InventoryPojo> getByBarcode(String barcode) throws ApiException {
        ProductPojo product = productApi.getProductByBarcode(barcode);
        InventoryPojo inventory = inventoryApi.getByProductId(product.getId());
        return Pair.of(product, inventory);
    }

    public Page<Pair<ProductPojo, InventoryPojo>> getAll(PageForm pageForm) {
        Page<ProductPojo> productPage = productApi.getAllProducts(pageForm.getPage(), pageForm.getSize());
        return attachInventoryToProductPage(productPage);
    }

    public Pair<ProductPojo, InventoryPojo> updateProduct(ProductUpdatePojo updateRequest) throws ApiException {
        ensureClientExistsByEmail(updateRequest.getClientEmail());

        ProductPojo updatedProduct = productApi.updateProduct(updateRequest);
        InventoryPojo inventory = inventoryApi.getByProductId(updatedProduct.getId());

        return Pair.of(updatedProduct, inventory);
    }

    @Transactional(rollbackFor = ApiException.class)
    public Pair<ProductPojo, InventoryPojo> updateInventory(InventoryUpdatePojo updateRequest) throws ApiException {
        ProductPojo product = productApi.getProductByBarcode(updateRequest.getBarcode());
        inventoryApi.createInventoryIfAbsent(product.getId());

        InventoryPojo inventoryToUpdate = new InventoryPojo();
        inventoryToUpdate.setProductId(product.getId());
        inventoryToUpdate.setQuantity(updateRequest.getQuantity());

        InventoryPojo updatedInventory = inventoryApi.updateInventory(inventoryToUpdate);
        return Pair.of(product, updatedInventory);
    }

    public Page<Pair<ProductPojo, InventoryPojo>> filter(ProductFilterForm filterForm) throws ApiException {
        List<String> matchedClientEmails = resolveClientEmailsForClientFilter(filterForm);

        Page<ProductPojo> productPage = productApi.filter(filterForm, matchedClientEmails);
        return attachInventoryToProductPage(productPage);
    }

    public List<String[]> bulkAddProducts(List<ProductPojo> incomingProducts) {
        List<String[]> resultRows = initializeBulkProductResultRows(incomingProducts);
        if (incomingProducts == null || incomingProducts.isEmpty()) {
            return resultRows;
        }

        Set<String> existingClientEmails = fetchExistingClientEmails(incomingProducts);
        Map<String, ProductPojo> existingProductByBarcode = fetchExistingProductsByBarcode(incomingProducts);

        BulkProductSavePlan savePlan = buildBulkProductSavePlan(
                incomingProducts,
                resultRows,
                existingClientEmails,
                existingProductByBarcode
        );

        if (savePlan.productsToSave().isEmpty()) {
            return resultRows;
        }

        Map<String, ProductPojo> savedProductByBarcode = saveProductsAndCreateInventories(savePlan.productsToSave());
        markSavedProductRowsSuccessful(resultRows, incomingProducts, savePlan.rowIndicesToSave(), savedProductByBarcode);

        return resultRows;
    }

    public List<String[]> bulkUpdateInventory(List<InventoryPojo> incomingInventoryRows) {
        List<String[]> resultRows = initializeBulkInventoryResultRows(incomingInventoryRows);
        if (incomingInventoryRows == null || incomingInventoryRows.isEmpty()) {
            return resultRows;
        }

        AggregatedInventoryInput aggregatedInput = aggregateInventoryDeltasByBarcode(incomingInventoryRows, resultRows);
        if (aggregatedInput.totalDeltaByBarcode().isEmpty()) {
            return resultRows;
        }

        Map<String, ProductPojo> productByBarcode = fetchProductsByBarcode(aggregatedInput.totalDeltaByBarcode().keySet());
        Map<String, InventoryPojo> inventoryByProductId = fetchInventoryByProductId(productByBarcode.values());

        List<InventoryPojo> inventoriesToSave = new ArrayList<>();
        applyAggregatedInventoryUpdates(
                aggregatedInput,
                productByBarcode,
                inventoryByProductId,
                inventoriesToSave,
                resultRows
        );

        if (!inventoriesToSave.isEmpty()) {
            inventoryApi.saveAll(inventoriesToSave);
        }

        return resultRows;
    }

    // -------------------- private helpers --------------------

    private void ensureClientExistsByEmail(String clientEmail) throws ApiException {
        clientApi.getClientByEmail(clientEmail);
    }

    private InventoryPojo getOrCreateInventoryForProductId(String productId) throws ApiException {
        inventoryApi.createInventoryIfAbsent(productId);
        return inventoryApi.getByProductId(productId);
    }

    private List<String> resolveClientEmailsForClientFilter(ProductFilterForm filterForm) {
        String clientFilter = filterForm.getClient();
        if (clientFilter == null || clientFilter.isBlank()) {
            return null;
        }
        return clientApi.findClientEmailsByNameOrEmail(clientFilter, CLIENT_MATCH_LIMIT);
    }

    private Set<String> fetchExistingClientEmails(List<ProductPojo> incomingProducts) {
        Set<String> requestedClientEmails = incomingProducts.stream()
                .filter(Objects::nonNull)
                .map(ProductPojo::getClientEmail)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (requestedClientEmails.isEmpty()) {
            return Set.of();
        }

        try {
            clientApi.validateClientsExist(requestedClientEmails);
            return requestedClientEmails;
        } catch (ApiException ignored) {
            return validateClientsOneByOne(requestedClientEmails);
        }
    }

    private Set<String> validateClientsOneByOne(Set<String> requestedClientEmails) {
        Set<String> existingClientEmails = new HashSet<>();
        for (String clientEmail : requestedClientEmails) {
            try {
                clientApi.getClientByEmail(clientEmail);
                existingClientEmails.add(clientEmail);
            } catch (ApiException ignored) {
            }
        }
        return existingClientEmails;
    }

    private Map<String, ProductPojo> fetchExistingProductsByBarcode(List<ProductPojo> incomingProducts) {
        List<String> incomingBarcodes = incomingProducts.stream()
                .filter(Objects::nonNull)
                .map(ProductPojo::getBarcode)
                .filter(Objects::nonNull)
                .toList();

        return productApi.findByBarcodes(incomingBarcodes)
                .stream()
                .collect(Collectors.toMap(ProductPojo::getBarcode, Function.identity()));
    }

    private BulkProductSavePlan buildBulkProductSavePlan(
            List<ProductPojo> incomingProducts,
            List<String[]> resultRows,
            Set<String> existingClientEmails,
            Map<String, ProductPojo> existingProductByBarcode
    ) {
        List<ProductPojo> productsToSave = new ArrayList<>();
        List<Integer> rowIndicesToSave = new ArrayList<>();

        for (int rowIndex = 0; rowIndex < incomingProducts.size(); rowIndex++) {
            ProductPojo incomingProduct = incomingProducts.get(rowIndex);
            if (incomingProduct == null) {
                continue;
            }

            if (!existingClientEmails.contains(incomingProduct.getClientEmail())) {
                resultRows.get(rowIndex)[2] = "Client does not exist";
                continue;
            }

            if (existingProductByBarcode.containsKey(incomingProduct.getBarcode())) {
                resultRows.get(rowIndex)[2] = "Duplicate barcode";
                continue;
            }

            productsToSave.add(incomingProduct);
            rowIndicesToSave.add(rowIndex);
        }

        return new BulkProductSavePlan(productsToSave, rowIndicesToSave);
    }

    private Map<String, ProductPojo> saveProductsAndCreateInventories(List<ProductPojo> productsToSave) {
        List<ProductPojo> savedProducts = productApi.saveAll(productsToSave);

        Map<String, ProductPojo> savedProductByBarcode = savedProducts.stream()
                .collect(Collectors.toMap(ProductPojo::getBarcode, Function.identity()));

        List<InventoryPojo> inventoriesToCreate = savedProducts.stream()
                .map(savedProduct -> {
                    InventoryPojo inventoryPojo = new InventoryPojo();
                    inventoryPojo.setProductId(savedProduct.getId());
                    inventoryPojo.setQuantity(0);
                    return inventoryPojo;
                })
                .toList();

        inventoryApi.saveAll(inventoriesToCreate);
        return savedProductByBarcode;
    }

    private void markSavedProductRowsSuccessful(
            List<String[]> resultRows,
            List<ProductPojo> incomingProducts,
            List<Integer> rowIndicesToSave,
            Map<String, ProductPojo> savedProductByBarcode
    ) {
        for (Integer rowIndex : rowIndicesToSave) {
            ProductPojo incomingProduct = incomingProducts.get(rowIndex);
            if (incomingProduct == null) continue;

            if (savedProductByBarcode.containsKey(incomingProduct.getBarcode())) {
                resultRows.get(rowIndex)[1] = "SUCCESS";
                resultRows.get(rowIndex)[2] = "";
            }
        }
    }

    private AggregatedInventoryInput aggregateInventoryDeltasByBarcode(List<InventoryPojo> incomingInventoryRows, List<String[]> resultRows) {
        Map<String, Integer> totalDeltaByBarcode = new LinkedHashMap<>();
        Map<String, List<Integer>> rowIndicesByBarcode = new HashMap<>();

        for (int rowIndex = 0; rowIndex < incomingInventoryRows.size(); rowIndex++) {
            InventoryPojo incomingRow = incomingInventoryRows.get(rowIndex);

            String barcode = incomingRow == null ? null : incomingRow.getProductId();
            Integer delta = incomingRow == null ? null : incomingRow.getQuantity();

            if (barcode == null || barcode.isBlank()) {
                resultRows.get(rowIndex)[2] = "Barcode cannot be empty";
                continue;
            }

            if (delta == null) {
                resultRows.get(rowIndex)[2] = "Quantity is required";
                continue;
            }

            if (delta < 0) {
                resultRows.get(rowIndex)[2] = "Quantity cannot be negative";
                continue;
            }

            totalDeltaByBarcode.merge(barcode, delta, Integer::sum);
            rowIndicesByBarcode.computeIfAbsent(barcode, ignored -> new ArrayList<>()).add(rowIndex);
        }

        return new AggregatedInventoryInput(totalDeltaByBarcode, rowIndicesByBarcode);
    }

    private Map<String, ProductPojo> fetchProductsByBarcode(Set<String> barcodes) {
        if (barcodes == null || barcodes.isEmpty()) {
            return Map.of();
        }

        return productApi.findByBarcodes(new ArrayList<>(barcodes))
                .stream()
                .collect(Collectors.toMap(ProductPojo::getBarcode, Function.identity()));
    }

    private Map<String, InventoryPojo> fetchInventoryByProductId(Collection<ProductPojo> products) {
        if (products == null || products.isEmpty()) {
            return Map.of();
        }

        List<String> productIds = products.stream().map(ProductPojo::getId).toList();
        return inventoryApi.getByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(InventoryPojo::getProductId, Function.identity()));
    }

    private void applyAggregatedInventoryUpdates(
            AggregatedInventoryInput aggregatedInput,
            Map<String, ProductPojo> productByBarcode,
            Map<String, InventoryPojo> inventoryByProductId,
            List<InventoryPojo> inventoriesToSave,
            List<String[]> resultRows
    ) {
        for (Map.Entry<String, Integer> entry : aggregatedInput.totalDeltaByBarcode().entrySet()) {
            String barcode = entry.getKey();
            int totalDelta = entry.getValue();

            List<Integer> rowIndices = aggregatedInput.rowIndicesByBarcode().getOrDefault(barcode, List.of());
            ProductPojo product = productByBarcode.get(barcode);

            if (product == null) {
                markBulkInventoryRowsError(resultRows, rowIndices, "Product not found");
                continue;
            }

            InventoryPojo inventory = inventoryByProductId.get(product.getId());
            if (inventory == null) {
                markBulkInventoryRowsError(resultRows, rowIndices, "Inventory not found for barcode: " + barcode);
                continue;
            }

            int currentQuantity = inventory.getQuantity() == null ? 0 : inventory.getQuantity();
            int nextQuantity = currentQuantity + totalDelta;

            if (nextQuantity > INVENTORY_MAX) {
                markBulkInventoryRowsError(resultRows, rowIndices, "Inventory cannot exceed " + INVENTORY_MAX);
                continue;
            }

            inventory.setQuantity(nextQuantity);
            inventoriesToSave.add(inventory);
            markBulkInventoryRowsSuccess(resultRows, rowIndices);
        }
    }

    private void markBulkInventoryRowsError(List<String[]> resultRows, List<Integer> rowIndices, String message) {
        for (Integer rowIndex : rowIndices) {
            resultRows.get(rowIndex)[1] = "ERROR";
            resultRows.get(rowIndex)[2] = message;
        }
    }

    private void markBulkInventoryRowsSuccess(List<String[]> resultRows, List<Integer> rowIndices) {
        for (Integer rowIndex : rowIndices) {
            resultRows.get(rowIndex)[1] = "SUCCESS";
            resultRows.get(rowIndex)[2] = "";
        }
    }

    private Page<Pair<ProductPojo, InventoryPojo>> attachInventoryToProductPage(Page<ProductPojo> productPage) {
        List<String> productIds = productPage.getContent().stream().map(ProductPojo::getId).toList();

        Map<String, InventoryPojo> inventoryByProductId = inventoryApi.getByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(InventoryPojo::getProductId, Function.identity()));

        List<Pair<ProductPojo, InventoryPojo>> combined = productPage.getContent().stream()
                .map(product -> Pair.of(product, inventoryByProductId.getOrDefault(product.getId(), new InventoryPojo())))
                .toList();

        return new PageImpl<>(combined, productPage.getPageable(), productPage.getTotalElements());
    }

    private List<String[]> initializeBulkProductResultRows(List<ProductPojo> incomingProducts) {
        if (incomingProducts == null) {
            return new ArrayList<>();
        }

        List<String[]> resultRows = new ArrayList<>(incomingProducts.size());
        for (ProductPojo incomingProduct : incomingProducts) {
            String barcode = incomingProduct == null ? "" : safeString(incomingProduct.getBarcode());
            resultRows.add(new String[]{barcode, "ERROR", ""});
        }
        return resultRows;
    }

    private List<String[]> initializeBulkInventoryResultRows(List<InventoryPojo> incomingInventoryRows) {
        if (incomingInventoryRows == null) {
            return new ArrayList<>();
        }

        List<String[]> resultRows = new ArrayList<>(incomingInventoryRows.size());
        for (InventoryPojo incomingRow : incomingInventoryRows) {
            String barcode = incomingRow == null ? "" : safeString(incomingRow.getProductId());
            resultRows.add(new String[]{barcode, "ERROR", ""});
        }
        return resultRows;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private record BulkProductSavePlan(List<ProductPojo> productsToSave, List<Integer> rowIndicesToSave) {}

    private record AggregatedInventoryInput(
            Map<String, Integer> totalDeltaByBarcode,
            Map<String, List<Integer>> rowIndicesByBarcode
    ) {}
}
