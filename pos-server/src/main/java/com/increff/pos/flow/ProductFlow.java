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

    public Pair<ProductPojo, InventoryPojo> addProduct(ProductPojo pojo) throws ApiException {
        clientApi.getClientByEmail(pojo.getClientEmail());
        ProductPojo saved = productApi.addProduct(pojo);
        inventoryApi.createInventoryIfAbsent(saved.getId());
        InventoryPojo inv = inventoryApi.getByProductId(saved.getId());
        return Pair.of(saved, inv);
    }

    public Pair<ProductPojo, InventoryPojo> getByBarcode(String barcode) throws ApiException {
        ProductPojo p = productApi.getProductByBarcode(barcode);
        InventoryPojo inv = inventoryApi.getByProductId(p.getId());
        return Pair.of(p, inv);
    }

    public Page<Pair<ProductPojo, InventoryPojo>> getAll(PageForm form) {
        Page<ProductPojo> products = productApi.getAllProducts(form.getPage(), form.getSize());
        return attachInventory(products);
    }

    public Pair<ProductPojo, InventoryPojo> updateProduct(ProductUpdatePojo updatePojo) throws ApiException {
        clientApi.getClientByEmail(updatePojo.getClientEmail());
        ProductPojo updated = productApi.updateProduct(updatePojo);
        InventoryPojo inv = inventoryApi.getByProductId(updated.getId());
        return Pair.of(updated, inv);
    }

    public Pair<ProductPojo, InventoryPojo> updateInventory(InventoryPojo invPojo) throws ApiException {
        InventoryPojo updatedInv = inventoryApi.updateInventory(invPojo);
        ProductPojo product = productApi.getProductById(updatedInv.getProductId());
        return Pair.of(product, updatedInv);
    }

    public Page<Pair<ProductPojo, InventoryPojo>> filter(ProductFilterForm form) throws ApiException {
        List<String> emails = (form.getClient() == null || form.getClient().isBlank())
                ? null
                : clientApi.filter(form.getClient(), form.getClient(), 0, 1000)
                .stream().map(c -> c.getEmail()).toList();
        Page<ProductPojo> filtered = productApi.filter(form, emails);
        return attachInventory(filtered);
    }

    public List<String[]> bulkAddProducts(List<ProductPojo> pojos) {
        List<String[]> results = initResultsForProducts(pojos);
        if (pojos == null || pojos.isEmpty()) return results;
        Set<String> emails = pojos.stream()
                .map(ProductPojo::getClientEmail)
                .collect(Collectors.toSet());

        Set<String> validClients = validateClients(emails);
        Map<String, ProductPojo> existingByBarcode = productApi.findByBarcodes(
                pojos.stream().map(ProductPojo::getBarcode).toList()
        ).stream().collect(Collectors.toMap(ProductPojo::getBarcode, Function.identity()));

        List<ProductPojo> toSave = new ArrayList<>();
        List<Integer> toSaveIndices = new ArrayList<>();
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
            toSaveIndices.add(i);
        }
        if (toSave.isEmpty()) return results;

        List<ProductPojo> saved = productApi.saveAll(toSave);
        Map<String, ProductPojo> savedByBarcode = saved.stream()
                .collect(Collectors.toMap(ProductPojo::getBarcode, Function.identity()));

        List<InventoryPojo> inventories = saved.stream().map(p -> {
            InventoryPojo i = new InventoryPojo();
            i.setProductId(p.getId());
            i.setQuantity(0);
            return i;
        }).toList();
        inventoryApi.saveAll(inventories);
        for (int k = 0; k < toSaveIndices.size(); k++) {
            int originalIndex = toSaveIndices.get(k);
            ProductPojo original = pojos.get(originalIndex);
            if (savedByBarcode.containsKey(original.getBarcode())) {
                results.get(originalIndex)[1] = "SUCCESS";
                results.get(originalIndex)[2] = "";
            }
        }

        return results;
    }

    public List<String[]> bulkUpdateInventory(List<InventoryPojo> pojos) {
        List<String[]> results = initResultsForInventory(pojos);
        if (pojos == null || pojos.isEmpty()) return results;
        List<String> barcodes = pojos.stream()
                .map(InventoryPojo::getProductId)
                .toList();

        Map<String, ProductPojo> productByBarcode = productApi.findByBarcodes(barcodes)
                .stream()
                .collect(Collectors.toMap(ProductPojo::getBarcode, Function.identity()));

        Map<String, InventoryPojo> inventoryByProductId = inventoryApi.getByProductIds(
                productByBarcode.values().stream().map(ProductPojo::getId).toList()
        ).stream().collect(Collectors.toMap(InventoryPojo::getProductId, Function.identity()));

        List<InventoryPojo> updates = new ArrayList<>();
        for (int i = 0; i < pojos.size(); i++) {
            InventoryPojo incoming = pojos.get(i);
            String barcode = incoming.getProductId();
            ProductPojo p = productByBarcode.get(barcode);
            if (p == null) {
                results.get(i)[2] = "Product not found";
                continue;
            }
            InventoryPojo inv = inventoryByProductId.get(p.getId());
            if (inv == null) {
                results.get(i)[2] = "Inventory not found";
                continue;
            }
            inv.setQuantity(inv.getQuantity() + incoming.getQuantity());
            updates.add(inv);
            results.get(i)[1] = "SUCCESS";
            results.get(i)[2] = "";
        }

        if (!updates.isEmpty()) inventoryApi.saveAll(updates);
        return results;
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
            results.add(new String[]{p.getProductId(), "ERROR", ""}); // barcode stored here
        }
        return results;
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
        ).stream().collect(Collectors.toMap(InventoryPojo::getProductId, Function.identity()));

        List<Pair<ProductPojo, InventoryPojo>> list = page.getContent().stream()
                .map(p -> Pair.of(p, invMap.getOrDefault(p.getId(), new InventoryPojo())))
                .toList();
        return new PageImpl<>(list, page.getPageable(), page.getTotalElements());
    }
}
