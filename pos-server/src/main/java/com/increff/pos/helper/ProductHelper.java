package com.increff.pos.helper;

import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.db.ProductUpdatePojo;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.form.ProductForm;
import com.increff.pos.model.form.ProductUpdateForm;

public class ProductHelper {
    public static ProductPojo convertFormToEntity(ProductForm form) {
        ProductPojo pojo = new ProductPojo();
        pojo.setBarcode(form.getBarcode());
        pojo.setClientId(form.getClientId());
        pojo.setName(form.getName());
        pojo.setMrp(form.getMrp());
        pojo.setImageUrl(form.getImageUrl());
        return pojo;
    }

    public static ProductUpdatePojo convertUpdateFormToEntity(
            ProductUpdateForm form) {

        ProductUpdatePojo pojo = new ProductUpdatePojo();
        pojo.setOldBarcode(form.getOldBarcode());
        pojo.setNewBarcode(form.getNewBarcode());
        pojo.setClientId(form.getClientId());
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
        data.setClientId(product.getClientId());
        data.setName(product.getName());
        data.setMrp(product.getMrp());
        data.setImageUrl(product.getImageUrl());
        data.setInventory(inventory.getQuantity());
        return data;
    }
}
