package com.increff.pos.flow;

import com.increff.pos.api.ClientApi;
import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.ProductSearchForm;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductFlow {
    private static final int INVENTORY_MAX = 1000;
    private static final int CLIENT_MATCH_LIMIT = 100;

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
        InventoryPojo inventory = inventoryApi.createInventoryIfAbsent(createdProduct.getId());
        return Pair.of(createdProduct, inventory);
    }

    public Pair<ProductPojo, InventoryPojo> getByBarcode(String barcode) throws ApiException {
        ProductPojo product = productApi.getProductByBarcode(barcode);
        InventoryPojo inventory = inventoryApi.getByProductId(product.getId());
        return Pair.of(product, inventory);
    }

    public Pair<ProductPojo, InventoryPojo> updateProduct(ProductPojo productToUpdate) throws ApiException {
        ensureClientExistsByEmail(productToUpdate.getClientEmail());
        ProductPojo updatedProduct = productApi.updateProduct(productToUpdate);
        InventoryPojo inventory = inventoryApi.getByProductId(updatedProduct.getId());
        return Pair.of(updatedProduct, inventory);
    }

    public Page<Pair<ProductPojo, InventoryPojo>> search(ProductSearchForm searchForm) throws ApiException {
        List<String> matchedClientEmails = resolveClientEmailsForClientSearch(searchForm);
        Page<ProductPojo> productPage = productApi.search(searchForm, matchedClientEmails);
        return attachInventoryToProductPage(productPage);
    }

    // ---------------- BULK ----------------

    public List<String[]> bulkAddProducts(List<ProductPojo> incomingProducts) {
        List<String> barcodeByRowIndexList = extractProductBarcodesByRowIndex(incomingProducts);
        List<String[]> resultRows = initializeBulkProductResultRows(barcodeByRowIndexList);

        if (incomingProducts == null || incomingProducts.isEmpty()) {
            return resultRows;
        }

        Set<String> existingClientEmailSet = fetchExistingClientEmails(incomingProducts);
        Map<String, ProductPojo> barcodeProductMap = fetchExistingProductsByBarcode(incomingProducts);

        BulkProductSavePlan savePlan = buildBulkProductSavePlan(
                incomingProducts,
                resultRows,
                existingClientEmailSet,
                barcodeProductMap
        );

        if (savePlan.productsToSave().isEmpty()) {
            return resultRows;
        }

        Map<String, ProductPojo> barcodeSavedProductMap = saveProductsAndCreateInventories(savePlan.productsToSave());
        markSavedProductRowsSuccessful(resultRows, incomingProducts, savePlan.rowIndicesToSave(), barcodeSavedProductMap);

        enforceBarcodeColumn(resultRows, barcodeByRowIndexList);
        return resultRows;
    }

    // -------------------- private helpers --------------------

    private void ensureClientExistsByEmail(String clientEmail) throws ApiException {
        clientApi.getClientByEmail(clientEmail);
    }

    private List<String> resolveClientEmailsForClientSearch(ProductSearchForm searchForm) {
        String clientSearch = searchForm.getClient();
        if (clientSearch == null || clientSearch.isBlank()) {
            return null;
        }
        return clientApi.findClientEmailsByNameOrEmail(clientSearch, CLIENT_MATCH_LIMIT);
    }

    private Set<String> fetchExistingClientEmails(List<ProductPojo> incomingProducts) {
        Set<String> requestedClientEmailSet = incomingProducts.stream()
                .filter(Objects::nonNull)
                .map(ProductPojo::getClientEmail)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (requestedClientEmailSet.isEmpty()) {
            return Set.of();
        }

        try {
            clientApi.validateClientsExist(requestedClientEmailSet);
            return requestedClientEmailSet;
        } catch (ApiException ignored) {
            return validateClientsOneByOne(requestedClientEmailSet);
        }
    }

    private Set<String> validateClientsOneByOne(Set<String> requestedClientEmailSet) {
        Set<String> existingClientEmailSet = new HashSet<>();
        for (String clientEmail : requestedClientEmailSet) {
            try {
                clientApi.getClientByEmail(clientEmail);
                existingClientEmailSet.add(clientEmail);
            } catch (ApiException ignored) {
            }
        }
        return existingClientEmailSet;
    }

    private Map<String, ProductPojo> fetchExistingProductsByBarcode(List<ProductPojo> incomingProducts) {
        List<String> incomingBarcodeList = incomingProducts.stream()
                .filter(Objects::nonNull)
                .map(ProductPojo::getBarcode)
                .filter(Objects::nonNull)
                .toList();

        return productApi.findByBarcodes(incomingBarcodeList)
                .stream()
                .collect(Collectors.toMap(ProductPojo::getBarcode, Function.identity()));
    }

    private BulkProductSavePlan buildBulkProductSavePlan(
            List<ProductPojo> incomingProducts,
            List<String[]> resultRows,
            Set<String> existingClientEmailSet,
            Map<String, ProductPojo> barcodeProductMap
    ) {
        List<ProductPojo> productsToSave = new ArrayList<>();
        List<Integer> rowIndicesToSave = new ArrayList<>();

        for (int rowIndex = 0; rowIndex < incomingProducts.size(); rowIndex++) {
            ProductPojo incomingProduct = incomingProducts.get(rowIndex);
            if (incomingProduct == null) {
                continue;
            }

            if (!existingClientEmailSet.contains(incomingProduct.getClientEmail())) {
                resultRows.get(rowIndex)[2] = "Client does not exist";
                continue;
            }

            if (barcodeProductMap.containsKey(incomingProduct.getBarcode())) {
                resultRows.get(rowIndex)[2] = "Duplicate barcode";
                continue;
            }

            productsToSave.add(incomingProduct);
            rowIndicesToSave.add(rowIndex);
        }

        return new BulkProductSavePlan(productsToSave, rowIndicesToSave);
    }

    private Map<String, ProductPojo> saveProductsAndCreateInventories(List<ProductPojo> productsToSave) {
        List<ProductPojo> savedProductList = productApi.saveAll(productsToSave);

        Map<String, ProductPojo> barcodeSavedProductMap = savedProductList.stream()
                .collect(Collectors.toMap(ProductPojo::getBarcode, Function.identity()));

        List<InventoryPojo> inventoryToCreateList = savedProductList.stream()
                .map(savedProduct -> {
                    InventoryPojo inventoryPojo = new InventoryPojo();
                    inventoryPojo.setProductId(savedProduct.getId());
                    inventoryPojo.setQuantity(0);
                    return inventoryPojo;
                })
                .toList();

        inventoryApi.saveAll(inventoryToCreateList);
        return barcodeSavedProductMap;
    }

    private void markSavedProductRowsSuccessful(
            List<String[]> resultRows,
            List<ProductPojo> incomingProducts,
            List<Integer> rowIndicesToSave,
            Map<String, ProductPojo> barcodeSavedProductMap
    ) {
        for (Integer rowIndex : rowIndicesToSave) {
            ProductPojo incomingProduct = incomingProducts.get(rowIndex);
            if (incomingProduct == null) continue;

            if (barcodeSavedProductMap.containsKey(incomingProduct.getBarcode())) {
                resultRows.get(rowIndex)[1] = "SUCCESS";
                resultRows.get(rowIndex)[2] = "";
            }
        }
    }

    private Page<Pair<ProductPojo, InventoryPojo>> attachInventoryToProductPage(Page<ProductPojo> productPage) {
        List<String> productIdList = productPage.getContent().stream().map(ProductPojo::getId).toList();

        Map<String, InventoryPojo> productIdInventoryMap = inventoryApi.getByProductIds(productIdList)
                .stream()
                .collect(Collectors.toMap(InventoryPojo::getProductId, Function.identity()));

        List<Pair<ProductPojo, InventoryPojo>> combined = productPage.getContent().stream()
                .map(product -> Pair.of(product, productIdInventoryMap.getOrDefault(product.getId(), new InventoryPojo())))
                .toList();

        return new PageImpl<>(combined, productPage.getPageable(), productPage.getTotalElements());
    }

    // -------------------- barcode-preserving bulk result helpers --------------------

    private List<String> extractProductBarcodesByRowIndex(List<ProductPojo> incomingProducts) {
        if (incomingProducts == null) return new ArrayList<>();
        List<String> barcodeByRowIndexList = new ArrayList<>(incomingProducts.size());
        for (ProductPojo p : incomingProducts) {
            barcodeByRowIndexList.add(p == null ? "" : safeString(p.getBarcode()));
        }
        return barcodeByRowIndexList;
    }

    private List<String[]> initializeBulkProductResultRows(List<String> barcodeByRowIndexList) {
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

    private record BulkProductSavePlan(List<ProductPojo> productsToSave, List<Integer> rowIndicesToSave) {}

    private record AggregatedInventoryInput(
            Map<String, Integer> totalDeltaByBarcode,
            Map<String, List<Integer>> rowIndicesByBarcode
    ) {}
}
