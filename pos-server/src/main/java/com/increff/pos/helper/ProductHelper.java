package com.increff.pos.helper;

import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.db.ProductUpdatePojo;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InventoryUpdateForm;
import com.increff.pos.model.form.ProductForm;
import com.increff.pos.model.form.ProductUpdateForm;
import com.increff.pos.util.ValidationUtil;

import java.util.*;

public class ProductHelper {

    public static ProductPojo convertProductFormToEntity(ProductForm form) {
        ProductPojo pojo = new ProductPojo();
        pojo.setBarcode(form.getBarcode());
        pojo.setClientEmail(form.getClientEmail());
        pojo.setName(form.getName());
        pojo.setMrp(form.getMrp());
        pojo.setImageUrl(form.getImageUrl());
        return pojo;
    }

    public static InventoryPojo convertInventoryUpdateFormToEntity(
            InventoryUpdateForm form) {

        InventoryPojo pojo = new InventoryPojo();
        pojo.setProductId(form.getProductId());
        pojo.setQuantity(form.getQuantity());
        return pojo;
    }

    public static ProductUpdatePojo convertProductUpdateFormToEntity(
            ProductUpdateForm form) {

        ProductUpdatePojo pojo = new ProductUpdatePojo();
        pojo.setOldBarcode(form.getOldBarcode());
        pojo.setNewBarcode(form.getNewBarcode());
        pojo.setClientEmail(form.getClientEmail());
        pojo.setName(form.getName());
        pojo.setMrp(form.getMrp());
        pojo.setImageUrl(form.getImageUrl());
        return pojo;
    }

    public static ProductData convertToProductData(
            ProductPojo product,
            InventoryPojo inventory) {

        ProductData data = new ProductData();
        data.setId(product.getId());
        data.setBarcode(product.getBarcode());
        data.setClientEmail(product.getClientEmail());
        data.setName(product.getName());
        data.setMrp(product.getMrp());
        data.setImageUrl(product.getImageUrl());
        data.setInventory(inventory.getQuantity());
        return data;
    }

    public static ProductPojo toBulkProductPojo(String[] row)
            throws ApiException {

        return ValidationUtil
                .convertBulkProductRowsToPojos(
                        Collections.singletonList(row)
                )
                .getFirst();
    }

    public static InventoryPojo toBulkInventoryPojo(String[] row)
            throws ApiException {

        return ValidationUtil
                .convertBulkInventoryRowsToPojos(
                        Collections.singletonList(row)
                )
                .getFirst();
    }
}
