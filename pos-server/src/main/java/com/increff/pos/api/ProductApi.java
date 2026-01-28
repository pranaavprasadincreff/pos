package com.increff.pos.api;

import com.increff.pos.db.ProductPojo;
import com.increff.pos.db.ProductUpdatePojo;
import com.increff.pos.model.exception.ApiException;
import org.springframework.data.domain.Page;

public interface ProductApi {
    ProductPojo addProduct(ProductPojo productPojo) throws ApiException;
    ProductPojo getProductById(String id) throws ApiException;
    ProductPojo getProductByBarcode(String barcode) throws ApiException;
    Page<ProductPojo> getAllProducts(int page, int size);
    ProductPojo updateProduct(ProductUpdatePojo updatePojo) throws ApiException;
}
