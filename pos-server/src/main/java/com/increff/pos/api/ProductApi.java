package com.increff.pos.api;

import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.ProductSearchForm;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductApi {
    ProductPojo addProduct(ProductPojo productToCreate) throws ApiException;
    ProductPojo getProductByBarcode(String barcode) throws ApiException;
    ProductPojo getById(String productId) throws ApiException;
    Page<ProductPojo> search(ProductSearchForm form, List<String> clientEmails);
    List<ProductPojo> findByBarcodes(List<String> barcodes);
    ProductPojo updateProduct(ProductPojo productToUpdate) throws ApiException;
    List<ProductPojo> saveAll(List<ProductPojo> productsToSave);
    List<ProductPojo> findByIds(List<String> productIds);
}
