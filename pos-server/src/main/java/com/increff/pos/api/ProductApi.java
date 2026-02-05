package com.increff.pos.api;

import com.increff.pos.db.ProductPojo;
import com.increff.pos.db.ProductUpdatePojo;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.ProductFilterForm;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductApi {
    ProductPojo addProduct(ProductPojo productPojo) throws ApiException;
    ProductPojo getProductByBarcode(String barcode) throws ApiException;
    Page<ProductPojo> getAllProducts(int page, int size);
    ProductPojo updateProduct(ProductUpdatePojo updatePojo) throws ApiException;
    Page<ProductPojo> filter(ProductFilterForm form, List<String> clientEmails);
    List<ProductPojo> findByBarcodes(List<String> barcodes);
    List<ProductPojo> saveAll(List<ProductPojo> list);
}
