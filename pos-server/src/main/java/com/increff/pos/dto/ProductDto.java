package com.increff.pos.dto;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.db.ProductUpdatePojo;
import com.increff.pos.exception.ApiException;
import com.increff.pos.helper.ProductHelper;
import com.increff.pos.helper.TsvHelper;
import com.increff.pos.model.data.BulkUploadData;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.form.*;
import com.increff.pos.util.ValidationUtil;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductDto {

    private final ProductApi productApi;
    private final InventoryApi inventoryApi;

    public ProductDto(ProductApi productApi, InventoryApi inventoryApi) {
        this.productApi = productApi;
        this.inventoryApi = inventoryApi;
    }

    public ProductData addProduct(ProductForm form) throws ApiException {
        ValidationUtil.validateProductForm(form);
        ProductPojo product = ProductHelper.convertProductFormToEntity(form);
        ProductPojo savedProduct = productApi.addProduct(product);
        InventoryPojo inventory = inventoryApi.getByProductId(savedProduct.getId());
        return ProductHelper.convertToProductData(savedProduct, inventory);
    }

    public ProductData getByBarcode(String barcode) throws ApiException {
        ProductPojo product = productApi.getProductByBarcode(barcode);
        InventoryPojo inventory = inventoryApi.getByProductId(product.getId());
        return ProductHelper.convertToProductData(product, inventory);
    }

    public Page<ProductData> getAll(PageForm form) throws ApiException {
        ValidationUtil.validatePageForm(form);
        return productApi
                .getAllProducts(form.getPage(), form.getSize())
                .map(this::attachInventory);
    }

    public ProductData updateProduct(ProductUpdateForm form) throws ApiException {
        ValidationUtil.validateProductUpdateForm(form);
        ProductUpdatePojo updatePojo = ProductHelper.convertProductUpdateFormToEntity(form);
        ProductPojo updatedProduct = productApi.updateProduct(updatePojo);
        InventoryPojo inventory = inventoryApi.getByProductId(updatedProduct.getId());
        return ProductHelper.convertToProductData(updatedProduct, inventory);
    }

    public ProductData updateInventory(InventoryUpdateForm form) throws ApiException {
        ValidationUtil.validateInventoryUpdateForm(form);
        InventoryPojo inventory = ProductHelper.convertInventoryUpdateFormToEntity(form);
        InventoryPojo updatedInventory = inventoryApi.updateInventory(inventory);
        ProductPojo product = productApi.getProductById(form.getProductId());
        return ProductHelper.convertToProductData(product, updatedInventory);
    }

    public BulkUploadData bulkAddProducts(BulkUploadForm form) throws ApiException {
        List<String[]> rows = TsvHelper.decode(form.getFile());
        List<String[]> result = new ArrayList<>();

        for (String[] row : rows) {
            if (isHeader(row)) continue;
            String barcode = row[0];
            try {
                ValidationUtil.validateBulkProductRow(row);
                ProductForm productForm = parseProductRow(row);
                ProductPojo product = ProductHelper.convertProductFormToEntity(productForm);
                productApi.addProduct(product);
                result.add(success(barcode, "Product added"));
            } catch (Exception e) {
                result.add(failure(barcode, e.getMessage()));
            }
        }
        return new BulkUploadData(TsvHelper.encodeResult(result));
    }

    public BulkUploadData bulkUpdateInventory(BulkUploadForm form) throws ApiException {
        List<String[]> rows = TsvHelper.decode(form.getFile());
        List<String[]> result = new ArrayList<>();

        for (String[] row : rows) {
            String barcode = row[0];
            try {
                int delta = ValidationUtil.validateBulkInventoryRow(row);
                ProductPojo product = productApi.getProductByBarcode(barcode);
                InventoryPojo inventory = inventoryApi.getByProductId(product.getId());
                inventoryApi.incrementInventory(inventory, delta);
                result.add(success(barcode, "Inventory updated"));
            } catch (ApiException e) {
                result.add(failure(barcode, e.getMessage()));
            }
        }
        return new BulkUploadData(TsvHelper.encodeResult(result)
        );
    }

    private ProductData attachInventory(ProductPojo product) {
        try {
            InventoryPojo inventory = inventoryApi.getByProductId(product.getId());
            return ProductHelper.convertToProductData(product, inventory);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private ProductForm parseProductRow(String[] row) {
        ProductForm form = new ProductForm();
        form.setBarcode(row[0]);
        form.setClientEmail(row[1]);
        form.setName(row[2]);
        form.setMrp(Double.parseDouble(row[3]));
        form.setImageUrl(row[4]);
        return form;
    }

    private boolean isHeader(String[] row) {
        return "barcode".equalsIgnoreCase(row[0]);
    }

    private String[] success(String barcode, String message) {
        return new String[]{barcode, "SUCCESS", message};
    }

    private String[] failure(String barcode, String message) {
        return new String[]{barcode, "FAILURE", message};
    }
}