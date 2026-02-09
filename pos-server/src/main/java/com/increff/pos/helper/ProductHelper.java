package com.increff.pos.helper;

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

    private ProductHelper() {
    }

    public static ProductPojo convertProductFormToEntity(ProductForm productCreateForm) {
        ProductPojo productToCreate = new ProductPojo();
        productToCreate.setBarcode(productCreateForm.getBarcode());
        productToCreate.setClientEmail(productCreateForm.getClientEmail());
        productToCreate.setName(productCreateForm.getName());
        productToCreate.setMrp(productCreateForm.getMrp());
        productToCreate.setImageUrl(productCreateForm.getImageUrl());
        return productToCreate;
    }

    public static InventoryPojo convertInventoryUpdateFormToInventoryPojo(InventoryUpdateForm form) {
        InventoryPojo inventoryPojo = new InventoryPojo();
        inventoryPojo.setQuantity(form.getQuantity());
        return inventoryPojo;
    }


    public static ProductPojo convertProductUpdateFormToProductPojo(ProductUpdateForm form) {
        ProductPojo p = new ProductPojo();
        p.setBarcode(form.getNewBarcode());
        p.setClientEmail(form.getClientEmail());
        p.setName(form.getName());
        p.setMrp(form.getMrp());
        p.setImageUrl(form.getImageUrl());
        return p;
    }

    public static ProductData convertToProductData(ProductPojo product, InventoryPojo inventory) {
        ProductData productData = new ProductData();
        productData.setBarcode(product.getBarcode());
        productData.setClientEmail(product.getClientEmail());
        productData.setName(product.getName());
        productData.setMrp(product.getMrp());
        productData.setImageUrl(product.getImageUrl());
        productData.setInventory(inventory == null ? null : inventory.getQuantity());
        return productData;
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
                .map(pair -> convertToProductData(pair.getLeft(), pair.getRight()))
                .toList();

        return new PageImpl<>(data, page.getPageable(), page.getTotalElements());
    }

    public static InventoryPojo createInventoryDeltaPojo(String barcode, Integer delta) {
        InventoryPojo inventoryPojo = new InventoryPojo();
        inventoryPojo.setProductId(barcode);
        inventoryPojo.setQuantity(delta);
        return inventoryPojo;
    }

    public static ProductPojo toBulkProductPojo(String[] canonicalRow) throws ApiException {
        validateBulkProductRowShape(canonicalRow);

        ProductPojo productPojo = new ProductPojo();
        productPojo.setBarcode(canonicalRow[0]);
        productPojo.setClientEmail(canonicalRow[1]);
        productPojo.setName(canonicalRow[2]);
        productPojo.setMrp(parseMrp(canonicalRow[3]));

        if (canonicalRow.length == 5) {
            productPojo.setImageUrl(canonicalRow[4]);
        }

        return productPojo;
    }

    public static InventoryPojo toBulkInventoryPojo(String[] canonicalRow) throws ApiException {
        validateBulkInventoryRowShape(canonicalRow);

        InventoryPojo inventoryPojo = new InventoryPojo();
        inventoryPojo.setProductId(canonicalRow[0]); // barcode carrier
        inventoryPojo.setQuantity(parseInventoryDelta(canonicalRow[1]));
        return inventoryPojo;
    }

    private static void validateBulkProductRowShape(String[] row) throws ApiException {
        if (row == null || row.length < 4 || row.length > 5) {
            throw new ApiException("Invalid product row");
        }
    }

    private static void validateBulkInventoryRowShape(String[] row) throws ApiException {
        if (row == null || row.length != 2) {
            throw new ApiException("Invalid inventory row");
        }
    }

    private static Double parseMrp(String value) throws ApiException {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            throw new ApiException("MRP must be a number");
        }
    }

    private static Integer parseInventoryDelta(String value) throws ApiException {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new ApiException("Quantity must be a valid integer");
        }
    }
}
