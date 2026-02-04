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
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

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

    public Pair<ProductPojo, InventoryPojo> addProduct(ProductPojo productToCreate) throws ApiException {
        ensureClientExists(productToCreate.getClientEmail());

        ProductPojo createdProduct = productApi.addProduct(productToCreate);
        InventoryPojo inventory = ensureInventoryExists(createdProduct.getId());

        return Pair.of(createdProduct, inventory);
    }

    public Pair<ProductPojo, InventoryPojo> getByBarcode(String barcode) throws ApiException {
        ProductPojo product = productApi.getProductByBarcode(barcode);
        InventoryPojo inventory = inventoryApi.getByProductId(product.getId());
        return Pair.of(product, inventory);
    }

    public Page<Pair<ProductPojo, InventoryPojo>> getAll(PageForm pageForm) {
        Page<ProductPojo> productsPage = productApi.getAllProducts(pageForm.getPage(), pageForm.getSize());
        return attachInventory(productsPage);
    }

    public Pair<ProductPojo, InventoryPojo> updateProduct(ProductUpdatePojo updateRequest) throws ApiException {
        ensureClientExists(updateRequest.getClientEmail());

        ProductPojo updatedProduct = productApi.updateProduct(updateRequest);
        InventoryPojo inventory = inventoryApi.getByProductId(updatedProduct.getId());

        return Pair.of(updatedProduct, inventory);
    }

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
        List<String> matchedClientEmails = resolveClientEmailsForFilter(filterForm);

        Page<ProductPojo> productsPage = productApi.filter(filterForm, matchedClientEmails);
        return attachInventory(productsPage);
    }

    public List<String[]> bulkAddProducts(List<ProductPojo> productsToCreate) {
        List<String[]> resultRows = initResultsForProducts(productsToCreate);
        if (productsToCreate == null || productsToCreate.isEmpty()) {
            return resultRows;
        }

        Set<String> validClientEmails = resolveValidClientEmails(productsToCreate);
        Map<String, ProductPojo> existingProductsByBarcode = fetchExistingProductsByBarcode(productsToCreate);

        BulkProductSavePlan savePlan = buildBulkProductSavePlan(productsToCreate, resultRows, validClientEmails, existingProductsByBarcode);
        if (savePlan.productsToSave().isEmpty()) {
            return resultRows;
        }

        Map<String, ProductPojo> savedByBarcode = saveProductsAndCreateInventories(savePlan.productsToSave());
        markSuccessfulProductRows(resultRows, productsToCreate, savePlan.rowIndicesToSave(), savedByBarcode);

        return resultRows;
    }

    /**
     * NEW BEHAVIOR:
     * - Allows duplicate barcodes in the incoming list
     * - Aggregates deltas per barcode
     * - Invalid rows do not block other rows
     * - If aggregated update would exceed INVENTORY_MAX, ALL rows for that barcode become error
     */
    public List<String[]> bulkUpdateInventory(List<InventoryPojo> incomingRows) {
        List<String[]> resultRows = initResultsForInventory(incomingRows);
        if (incomingRows == null || incomingRows.isEmpty()) {
            return resultRows;
        }

        AggregatedInventoryInput aggregatedInput = aggregateInventoryDeltas(incomingRows, resultRows);

        if (aggregatedInput.totalDeltaByBarcode().isEmpty()) {
            return resultRows;
        }

        Map<String, ProductPojo> productByBarcode = fetchProductsByBarcode(aggregatedInput.totalDeltaByBarcode().keySet());
        Map<String, InventoryPojo> inventoryByProductId = fetchInventoriesByProductId(productByBarcode.values());

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

    // -------------------- Private helpers (max 2 levels) --------------------

    private void ensureClientExists(String clientEmail) throws ApiException {
        clientApi.getClientByEmail(clientEmail);
    }

    private InventoryPojo ensureInventoryExists(String productId) throws ApiException {
        inventoryApi.createInventoryIfAbsent(productId);
        return inventoryApi.getByProductId(productId);
    }

    /**
     * Product filter requirement:
     * - form.getClient() should match client NAME OR EMAIL (case-insensitive, partial)
     * - keep existing client filter behavior unchanged
     */
    private List<String> resolveClientEmailsForFilter(ProductFilterForm form) {
        if (form.getClient() == null || form.getClient().isBlank()) {
            return null;
        }
        return clientApi.findClientEmailsByNameOrEmail(form.getClient(), CLIENT_MATCH_LIMIT);
    }

    private Set<String> resolveValidClientEmails(List<ProductPojo> productsToCreate) {
        Set<String> clientEmailsInFile = productsToCreate.stream()
                .map(ProductPojo::getClientEmail)
                .collect(Collectors.toSet());

        if (clientEmailsInFile.isEmpty()) {
            return Set.of();
        }

        try {
            clientApi.validateClientsExist(clientEmailsInFile);
            return clientEmailsInFile;
        } catch (ApiException ignored) {
            return validateClientsIndividually(clientEmailsInFile);
        }
    }

    private Set<String> validateClientsIndividually(Set<String> emails) {
        Set<String> valid = new HashSet<>();
        for (String email : emails) {
            try {
                clientApi.getClientByEmail(email);
                valid.add(email);
            } catch (ApiException ignored) {
            }
        }
        return valid;
    }

    private Map<String, ProductPojo> fetchExistingProductsByBarcode(List<ProductPojo> products) {
        List<String> barcodes = products.stream().map(ProductPojo::getBarcode).toList();
        return productApi.findByBarcodes(barcodes)
                .stream()
                .collect(Collectors.toMap(ProductPojo::getBarcode, Function.identity()));
    }

    private BulkProductSavePlan buildBulkProductSavePlan(
            List<ProductPojo> productsToCreate,
            List<String[]> resultRows,
            Set<String> validClients,
            Map<String, ProductPojo> existingByBarcode
    ) {
        List<ProductPojo> toSave = new ArrayList<>();
        List<Integer> rowIndicesToSave = new ArrayList<>();

        for (int i = 0; i < productsToCreate.size(); i++) {
            ProductPojo product = productsToCreate.get(i);

            if (!validClients.contains(product.getClientEmail())) {
                resultRows.get(i)[2] = "Client does not exist";
                continue;
            }

            if (existingByBarcode.containsKey(product.getBarcode())) {
                resultRows.get(i)[2] = "Duplicate barcode";
                continue;
            }

            toSave.add(product);
            rowIndicesToSave.add(i);
        }

        return new BulkProductSavePlan(toSave, rowIndicesToSave);
    }

    private Map<String, ProductPojo> saveProductsAndCreateInventories(List<ProductPojo> productsToSave) {
        List<ProductPojo> savedProducts = productApi.saveAll(productsToSave);

        Map<String, ProductPojo> savedByBarcode = savedProducts.stream()
                .collect(Collectors.toMap(ProductPojo::getBarcode, Function.identity()));

        List<InventoryPojo> inventoriesToCreate = savedProducts.stream()
                .map(product -> {
                    InventoryPojo inventory = new InventoryPojo();
                    inventory.setProductId(product.getId());
                    inventory.setQuantity(0);
                    return inventory;
                })
                .toList();

        inventoryApi.saveAll(inventoriesToCreate);
        return savedByBarcode;
    }

    private void markSuccessfulProductRows(
            List<String[]> resultRows,
            List<ProductPojo> originalProducts,
            List<Integer> rowIndicesToSave,
            Map<String, ProductPojo> savedByBarcode
    ) {
        for (Integer rowIndex : rowIndicesToSave) {
            ProductPojo original = originalProducts.get(rowIndex);
            if (savedByBarcode.containsKey(original.getBarcode())) {
                resultRows.get(rowIndex)[1] = "SUCCESS";
                resultRows.get(rowIndex)[2] = "";
            }
        }
    }

    private AggregatedInventoryInput aggregateInventoryDeltas(List<InventoryPojo> incomingRows, List<String[]> resultRows) {
        Map<String, Integer> totalDeltaByBarcode = new LinkedHashMap<>();
        Map<String, List<Integer>> rowIndicesByBarcode = new HashMap<>();

        for (int i = 0; i < incomingRows.size(); i++) {
            InventoryPojo incoming = incomingRows.get(i);
            String barcode = incoming == null ? null : incoming.getProductId(); // barcode carrier
            Integer delta = incoming == null ? null : incoming.getQuantity();

            if (barcode == null || barcode.isBlank()) {
                resultRows.get(i)[2] = "Barcode cannot be empty";
                continue;
            }

            if (delta == null) {
                resultRows.get(i)[2] = "Quantity is required";
                continue;
            }

            if (delta < 0) {
                resultRows.get(i)[2] = "Quantity cannot be negative";
                continue;
            }

            totalDeltaByBarcode.merge(barcode, delta, Integer::sum);
            rowIndicesByBarcode.computeIfAbsent(barcode, k -> new ArrayList<>()).add(i);
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

    private Map<String, InventoryPojo> fetchInventoriesByProductId(Collection<ProductPojo> products) {
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
                markInventoryRowsError(resultRows, rowIndices, "Product not found");
                continue;
            }

            InventoryPojo inventory = inventoryByProductId.get(product.getId());
            if (inventory == null) {
                markInventoryRowsError(resultRows, rowIndices, "Inventory not found for barcode: " + barcode);
                continue;
            }

            int current = inventory.getQuantity() == null ? 0 : inventory.getQuantity();
            int next = current + totalDelta;

            if (next > INVENTORY_MAX) {
                markInventoryRowsError(resultRows, rowIndices, "Inventory cannot exceed " + INVENTORY_MAX);
                continue;
            }

            inventory.setQuantity(next);
            inventoriesToSave.add(inventory);
            markInventoryRowsSuccess(resultRows, rowIndices);
        }
    }

    private void markInventoryRowsError(List<String[]> resultRows, List<Integer> rowIndices, String message) {
        for (Integer idx : rowIndices) {
            resultRows.get(idx)[1] = "ERROR";
            resultRows.get(idx)[2] = message;
        }
    }

    private void markInventoryRowsSuccess(List<String[]> resultRows, List<Integer> rowIndices) {
        for (Integer idx : rowIndices) {
            resultRows.get(idx)[1] = "SUCCESS";
            resultRows.get(idx)[2] = "";
        }
    }

    private Page<Pair<ProductPojo, InventoryPojo>> attachInventory(Page<ProductPojo> productPage) {
        List<String> productIds = productPage.getContent().stream().map(ProductPojo::getId).toList();

        Map<String, InventoryPojo> inventoryByProductId = inventoryApi.getByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(InventoryPojo::getProductId, Function.identity()));

        List<Pair<ProductPojo, InventoryPojo>> combined = productPage.getContent().stream()
                .map(product -> Pair.of(product, inventoryByProductId.getOrDefault(product.getId(), new InventoryPojo())))
                .toList();

        return new PageImpl<>(combined, productPage.getPageable(), productPage.getTotalElements());
    }

    private List<String[]> initResultsForProducts(List<ProductPojo> products) {
        if (products == null) {
            return new ArrayList<>();
        }

        List<String[]> result = new ArrayList<>(products.size());
        for (ProductPojo product : products) {
            result.add(new String[]{product.getBarcode(), "ERROR", ""});
        }
        return result;
    }

    private List<String[]> initResultsForInventory(List<InventoryPojo> incomingRows) {
        if (incomingRows == null) {
            return new ArrayList<>();
        }

        List<String[]> result = new ArrayList<>(incomingRows.size());
        for (InventoryPojo row : incomingRows) {
            String barcode = row == null ? "" : row.getProductId(); // barcode carrier
            result.add(new String[]{barcode == null ? "" : barcode, "ERROR", ""});
        }
        return result;
    }

    private record BulkProductSavePlan(List<ProductPojo> productsToSave, List<Integer> rowIndicesToSave) {}

    private record AggregatedInventoryInput(
            Map<String, Integer> totalDeltaByBarcode,
            Map<String, List<Integer>> rowIndicesByBarcode
    ) {}
}
