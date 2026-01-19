package com.increff.pos.dto;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.db.ProductUpdatePojo;
import com.increff.pos.exception.ApiException;
import com.increff.pos.helper.ProductHelper;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.form.InventoryUpdateForm;
import com.increff.pos.model.form.ProductForm;
import com.increff.pos.model.form.ProductUpdateForm;
import com.increff.pos.model.form.PageForm;
import com.increff.pos.util.ValidationUtil;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

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
        ProductPojo productPojo = ProductHelper.convertFormToEntity(form);
        ProductPojo savedProduct = productApi.addProduct(productPojo);

        InventoryPojo inventoryPojo = new InventoryPojo();
        inventoryPojo.setProductId(savedProduct.getId());
        inventoryPojo.setQuantity(0);
        inventoryApi.addInventory(inventoryPojo);
        return ProductHelper.convertToProductData(
                savedProduct,
                inventoryPojo
        );
    }

    public ProductData getByBarcode(String barcode) throws ApiException {
        ProductPojo product = productApi.getProductByBarcode(barcode);
        InventoryPojo inventory = inventoryApi.getByProductId(product.getId());
        return ProductHelper.convertToProductData(product, inventory);
    }

    public Page<ProductData> getAll(PageForm form) throws ApiException {
        ValidationUtil.validatePageForm(form);
        Page<ProductPojo> page = productApi.getAllProducts(form.getPage(), form.getSize());
        return page.map(product -> {
            try {
                InventoryPojo inventory = inventoryApi.getByProductId(product.getId());
                return ProductHelper.convertToProductData(product, inventory);
            } catch (ApiException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public ProductData updateProduct(ProductUpdateForm form) throws ApiException {
        ValidationUtil.validateProductUpdateForm(form);
        ProductUpdatePojo updatePojo = ProductHelper.convertUpdateFormToEntity(form);
        ProductPojo updated = productApi.updateProduct(updatePojo);
        InventoryPojo inventory = inventoryApi.getByProductId(updated.getId());
        return ProductHelper.convertToProductData(updated, inventory);
    }

    public ProductData updateInventory(InventoryUpdateForm form) throws ApiException {
        ValidationUtil.validateInventoryUpdateForm(form);
        InventoryPojo inventory = inventoryApi.getByProductId(form.getProductId());
        inventory.setQuantity(form.getQuantity());
        InventoryPojo updatedInventory = inventoryApi.updateInventory(inventory);
        ProductPojo product = productApi.getProductById(form.getProductId());
        return ProductHelper.convertToProductData(product, updatedInventory);
    }
}
