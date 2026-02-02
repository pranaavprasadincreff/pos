package com.increff.pos.flow;

import com.increff.pos.api.ClientApi;
import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.InventoryPojo;
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
    @Autowired
    private ProductApi productApi;
    @Autowired
    private InventoryApi inventoryApi;
    @Autowired
    private ClientApi clientApi;

    private static final int INVENTORY_MAX = 1000;

    public Pair<ProductPojo, InventoryPojo> addProduct(ProductPojo pojo) throws ApiException {
        ensureClientExists(pojo.getClientEmail());
        ProductPojo saved = productApi.addProduct(pojo);
        InventoryPojo inv = ensureInventoryExists(saved.getId());
        return Pair.of(saved, inv);
    }

    public Pair<ProductPojo, InventoryPojo> getByBarcode(String barcode) throws ApiException {
        ProductPojo product = productApi.getProductByBarcode(barcode);
        InventoryPojo inventory = inventoryApi.getByProductId(product.getId());
        return Pair.of(product, inventory);
    }

    public Page<Pair<ProductPojo, InventoryPojo>> getAll(PageForm form) {
        Page<ProductPojo> products = productApi.getAllProducts(form.getPage(), form.getSize());
        return attachInventory(products);
    }

    public Pair<ProductPojo, InventoryPojo> updateProduct(ProductUpdatePojo updatePojo) throws ApiException {
        ensureClientExists(updatePojo.getClientEmail());
        ProductPojo updated = productApi.updateProduct(updatePojo);
        InventoryPojo inv = inventoryApi.getByProductId(updated.getId());
        return Pair.of(updated, inv);
    }

    public Pair<ProductPojo, InventoryPojo> updateInventory(InventoryPojo invPojo) throws ApiException {
        validateInventoryCap(invPojo.getQuantity());
        ProductPojo product = resolveProductForInventoryUpdate(invPojo);
        InventoryPojo updatedInv = persistInventoryUpdate(product, invPojo.getQuantity());
        return Pair.of(product, updatedInv);
    }

    public Page<Pair<ProductPojo, InventoryPojo>> filter(ProductFilterForm form) throws ApiException {
        List<String> clientEmails = resolveClientEmailsForFilter(form);
        Page<ProductPojo> filtered = productApi.filter(form, clientEmails);
        return attachInventory(filtered);
    }

    public List<String[]> bulkAddProducts(List<ProductPojo> pojos) {
        List<String[]> results = initResultsForProducts(pojos);
        if (pojos == null || pojos.isEmpty()) return results;

        Set<String> validClients = validateClients(extractClientEmails(pojos));
        Map<String, ProductPojo> existingByBarcode = getExistingProductsByBarcode(pojos);

        BulkProductSavePlan plan = planBulkProductSaves(pojos, results, validClients, existingByBarcode);
        if (plan.toSave().isEmpty()) return results;

        Map<String, ProductPojo> savedByBarcode = saveProductsAndCreateInventories(plan.toSave());
        markSuccessfulProductRows(results, pojos, plan.indicesToSave(), savedByBarcode);
        return results;
    }

    public List<String[]> bulkUpdateInventory(List<InventoryPojo> pojos) {
        List<String[]> results = initResultsForInventory(pojos);
        if (pojos == null || pojos.isEmpty()) return results;

        Map<String, ProductPojo> productByBarcode = resolveProductsForBulkInventory(pojos);
        Map<String, InventoryPojo> inventoryByProductId = resolveInventoriesForBulkInventory(productByBarcode);

        List<InventoryPojo> updates = applyBulkInventoryUpdates(pojos, results, productByBarcode, inventoryByProductId);

        if (!updates.isEmpty()) inventoryApi.saveAll(updates);
        return results;
    }

    private void ensureClientExists(String clientEmail) throws ApiException {
        clientApi.getClientByEmail(clientEmail);
    }

    private InventoryPojo ensureInventoryExists(String productId) throws ApiException {
        inventoryApi.createInventoryIfAbsent(productId);
        return inventoryApi.getByProductId(productId);
    }

    private void validateInventoryCap(Integer qty) throws ApiException {
        if (qty != null && qty > INVENTORY_MAX) {
            throw new ApiException("Inventory cannot exceed " + INVENTORY_MAX);
        }
    }

    private ProductPojo resolveProductForInventoryUpdate(InventoryPojo invPojo) throws ApiException {
        return productApi.getProductById(invPojo.getProductId());
    }

    private InventoryPojo persistInventoryUpdate(ProductPojo product, Integer qty) throws ApiException {
        InventoryPojo toUpdate = new InventoryPojo();
        toUpdate.setProductId(product.getId());
        toUpdate.setQuantity(qty);

        try {
            return inventoryApi.updateInventory(toUpdate);
        } catch (ApiException e) {
            throw new ApiException("Inventory not found for barcode: " + product.getBarcode());
        }
    }

    private List<String> resolveClientEmailsForFilter(ProductFilterForm form) throws ApiException {
        if (form.getClient() == null || form.getClient().isBlank()) return null;

        return clientApi.filter(form.getClient(), form.getClient(), 0, 1000)
                .stream()
                .map(c -> c.getEmail())
                .toList();
    }

    private Set<String> extractClientEmails(List<ProductPojo> pojos) {
        return pojos.stream()
                .map(ProductPojo::getClientEmail)
                .collect(Collectors.toSet());
    }

    private Map<String, ProductPojo> getExistingProductsByBarcode(List<ProductPojo> pojos) {
        List<String> barcodes = pojos.stream().map(ProductPojo::getBarcode).toList();
        return productApi.findByBarcodes(barcodes)
                .stream()
                .collect(Collectors.toMap(ProductPojo::getBarcode, Function.identity()));
    }

    private BulkProductSavePlan planBulkProductSaves(
            List<ProductPojo> pojos,
            List<String[]> results,
            Set<String> validClients,
            Map<String, ProductPojo> existingByBarcode
    ) {
        List<ProductPojo> toSave = new ArrayList<>();
        List<Integer> indicesToSave = new ArrayList<>();

        for (int i = 0; i < pojos.size(); i++) {
            ProductPojo p = pojos.get(i);

            if (!validClients.contains(p.getClientEmail())) {
                results.get(i)[2] = "Client does not exist";
                continue;
            }
            if (existingByBarcode.containsKey(p.getBarcode())) {
                results.get(i)[2] = "Duplicate barcode";
                continue;
            }

            toSave.add(p);
            indicesToSave.add(i);
        }

        return new BulkProductSavePlan(toSave, indicesToSave);
    }

    private Map<String, ProductPojo> saveProductsAndCreateInventories(List<ProductPojo> toSave) {
        List<ProductPojo> saved = productApi.saveAll(toSave);
        Map<String, ProductPojo> savedByBarcode = saved.stream()
                .collect(Collectors.toMap(ProductPojo::getBarcode, Function.identity()));

        List<InventoryPojo> inventories = saved.stream()
                .map(p -> {
                    InventoryPojo i = new InventoryPojo();
                    i.setProductId(p.getId());
                    i.setQuantity(0);
                    return i;
                })
                .toList();

        inventoryApi.saveAll(inventories);
        return savedByBarcode;
    }

    private void markSuccessfulProductRows(
            List<String[]> results,
            List<ProductPojo> originalPojos,
            List<Integer> indicesToSave,
            Map<String, ProductPojo> savedByBarcode
    ) {
        for (int k = 0; k < indicesToSave.size(); k++) {
            int originalIndex = indicesToSave.get(k);
            ProductPojo original = originalPojos.get(originalIndex);

            if (savedByBarcode.containsKey(original.getBarcode())) {
                results.get(originalIndex)[1] = "SUCCESS";
                results.get(originalIndex)[2] = "";
            }
        }
    }

    private Map<String, ProductPojo> resolveProductsForBulkInventory(List<InventoryPojo> pojos) {
        List<String> barcodes = pojos.stream().map(InventoryPojo::getProductId).toList(); // barcode carrier
        return productApi.findByBarcodes(barcodes)
                .stream()
                .collect(Collectors.toMap(ProductPojo::getBarcode, Function.identity()));
    }

    private Map<String, InventoryPojo> resolveInventoriesForBulkInventory(Map<String, ProductPojo> productByBarcode) {
        List<String> productIds = productByBarcode.values().stream().map(ProductPojo::getId).toList();
        return inventoryApi.getByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(InventoryPojo::getProductId, Function.identity()));
    }

    private List<InventoryPojo> applyBulkInventoryUpdates(
            List<InventoryPojo> incomingRows,
            List<String[]> results,
            Map<String, ProductPojo> productByBarcode,
            Map<String, InventoryPojo> inventoryByProductId
    ) {
        List<InventoryPojo> updates = new ArrayList<>();

        for (int i = 0; i < incomingRows.size(); i++) {
            InventoryPojo incoming = incomingRows.get(i);
            String barcode = incoming.getProductId();

            ProductPojo p = productByBarcode.get(barcode);
            if (p == null) {
                results.get(i)[2] = "Product not found";
                continue;
            }

            InventoryPojo inv = inventoryByProductId.get(p.getId());
            if (inv == null) {
                results.get(i)[2] = "Inventory not found for barcode: " + barcode;
                continue;
            }

            int current = inv.getQuantity() == null ? 0 : inv.getQuantity();
            int delta = incoming.getQuantity() == null ? 0 : incoming.getQuantity();
            int next = current + delta;

            if (next > INVENTORY_MAX) {
                results.get(i)[2] = "Inventory cannot exceed " + INVENTORY_MAX;
                continue;
            }

            inv.setQuantity(next);
            updates.add(inv);

            results.get(i)[1] = "SUCCESS";
            results.get(i)[2] = "";
        }
        return updates;
    }

    private Set<String> validateClients(Set<String> emails) {
        Set<String> validClients = new HashSet<>();
        if (emails == null || emails.isEmpty()) return validClients;

        try {
            clientApi.validateClientsExist(emails);
            validClients.addAll(emails);
            return validClients;
        } catch (ApiException ignored) {
            for (String email : emails) {
                try {
                    clientApi.getClientByEmail(email);
                    validClients.add(email);
                } catch (ApiException ignored2) {
                }
            }
            return validClients;
        }
    }

    private Page<Pair<ProductPojo, InventoryPojo>> attachInventory(Page<ProductPojo> page) {
        Map<String, InventoryPojo> invMap = inventoryApi.getByProductIds(
                        page.getContent().stream().map(ProductPojo::getId).toList()
                ).stream()
                .collect(Collectors.toMap(InventoryPojo::getProductId, Function.identity()));

        List<Pair<ProductPojo, InventoryPojo>> list = page.getContent().stream()
                .map(p -> Pair.of(p, invMap.getOrDefault(p.getId(), new InventoryPojo())))
                .toList();

        return new PageImpl<>(list, page.getPageable(), page.getTotalElements());
    }

    private List<String[]> initResultsForProducts(List<ProductPojo> pojos) {
        if (pojos == null) return new ArrayList<>();
        List<String[]> results = new ArrayList<>(pojos.size());
        for (ProductPojo p : pojos) {
            results.add(new String[]{p.getBarcode(), "ERROR", ""});
        }
        return results;
    }

    private List<String[]> initResultsForInventory(List<InventoryPojo> pojos) {
        if (pojos == null) return new ArrayList<>();
        List<String[]> results = new ArrayList<>(pojos.size());
        for (InventoryPojo p : pojos) {
            results.add(new String[]{p.getProductId(), "ERROR", ""});
        }
        return results;
    }

    private record BulkProductSavePlan(List<ProductPojo> toSave, List<Integer> indicesToSave) {}
}
