package com.increff.pos.helper;

import com.increff.pos.util.ValidationUtil;
import org.apache.commons.lang3.tuple.Pair;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InventoryUpdateForm;
import com.increff.pos.model.form.ProductForm;
import com.increff.pos.model.form.ProductUpdateForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

public class ProductHelper {

    public static ProductPojo convertProductFormToEntity(ProductForm productCreateForm) {
        ProductPojo productToCreate = new ProductPojo();
        productToCreate.setBarcode(productCreateForm.getBarcode());
        productToCreate.setClientEmail(productCreateForm.getClientEmail());
        productToCreate.setName(productCreateForm.getName());
        productToCreate.setMrp(productCreateForm.getMrp());
        productToCreate.setImageUrl(productCreateForm.getImageUrl());
        return productToCreate;
    }

    public static InventoryPojo convertInventoryUpdateFormToInventoryPojo(
            Pair<ProductPojo, InventoryPojo> existing,
            InventoryUpdateForm form
    ) {
        InventoryPojo inventory = new InventoryPojo();
        inventory.setProductId(existing.getLeft().getId());
        inventory.setQuantity(form.getQuantity());
        return inventory;
    }

    public static ProductPojo convertProductUpdateFormToProductPojo(
            Pair<ProductPojo, InventoryPojo> existing,
            ProductUpdateForm form
    ) {
        ProductPojo product = existing.getLeft();
        product.setBarcode(form.getNewBarcode());
        product.setClientEmail(form.getClientEmail());
        product.setName(form.getName());
        product.setMrp(form.getMrp());
        product.setImageUrl(form.getImageUrl());
        return product;
    }

    public static ProductData convertToProductData(
            Pair<ProductPojo, InventoryPojo> pair
    ) {
        ProductPojo product = pair.getLeft();
        InventoryPojo inventory = pair.getRight();

        ProductData data = new ProductData();
        data.setBarcode(product.getBarcode());
        data.setClientEmail(product.getClientEmail());
        data.setName(product.getName());
        data.setMrp(product.getMrp());
        data.setImageUrl(product.getImageUrl());
        data.setInventory(inventory == null ? null : inventory.getQuantity());

        return data;
    }

    // ---------------- BULK HELPERS ----------------

    public static ProductPojo toBulkProductPojo(String[] canonicalProductRow) throws ApiException {
        Double mrp = ValidationUtil.getCheckMrp(canonicalProductRow[3]);
        return toBulkProductPojo(canonicalProductRow, mrp);
    }

    public static ProductPojo toBulkProductPojo(String[] canonicalProductRow, Double mrp) {
        String barcode = canonicalProductRow[0];
        String clientEmail = canonicalProductRow[1];
        String name = canonicalProductRow[2];
        String imageUrl = canonicalProductRow.length == 5 ? canonicalProductRow[4] : null;

        return createProductPojo(barcode, clientEmail, name, mrp, imageUrl);
    }

    public static InventoryPojo toBulkInventoryDeltaPojo(String[] canonicalInventoryRow) throws ApiException {
        int delta = ValidationUtil.getCheckInventoryDelta(canonicalInventoryRow[1]);
        return createInventoryDeltaPojo(canonicalInventoryRow[0], delta);
    }

    public static ProductPojo createProductPojo(String barcode, String clientEmail, String name, Double mrp, String imageUrl) {
        ProductPojo productPojo = new ProductPojo();
        productPojo.setBarcode(barcode);
        productPojo.setClientEmail(clientEmail);
        productPojo.setName(name);
        productPojo.setMrp(mrp);
        productPojo.setImageUrl(imageUrl);
        return productPojo;
    }

    public static Page<ProductData> convertToProductDataPage(Page<Pair<ProductPojo, InventoryPojo>> page) {
        List<ProductData> data = page.getContent()
                .stream()
                .map(ProductHelper::convertToProductData)
                .toList();

        return new PageImpl<>(data, page.getPageable(), page.getTotalElements());
    }

    public static InventoryPojo createInventoryDeltaPojo(String barcode, Integer delta) {
        InventoryPojo inventoryPojo = new InventoryPojo();
        inventoryPojo.setProductId(barcode);
        inventoryPojo.setQuantity(delta);
        return inventoryPojo;
    }
}
