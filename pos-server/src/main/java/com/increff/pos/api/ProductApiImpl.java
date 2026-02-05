package com.increff.pos.api;

import com.increff.pos.dao.ProductDao;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.db.ProductUpdatePojo;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.ProductFilterForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductApiImpl implements ProductApi {

    @Autowired
    private ProductDao productDao;

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public ProductPojo addProduct(ProductPojo productToCreate) throws ApiException {
        ensureBarcodeIsUnique(productToCreate.getBarcode(), null);
        return productDao.save(productToCreate);
    }

    @Override
    public ProductPojo getProductByBarcode(String barcode) throws ApiException {
        return loadProductByBarcode(barcode);
    }

    @Override
    public Page<ProductPojo> getAllProducts(int page, int size) {
        return productDao.findAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
    }

    @Override
    public Page<ProductPojo> filter(ProductFilterForm form, List<String> clientEmails) {
        return productDao.filter(form, clientEmails);
    }

    @Override
    public List<ProductPojo> findByBarcodes(List<String> barcodes) {
        return productDao.findByBarcodes(barcodes);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public ProductPojo updateProduct(ProductUpdatePojo updateRequest) throws ApiException {
        ProductPojo existingProduct = loadProductByBarcode(updateRequest.getOldBarcode());

        ensureBarcodeIsUnique(updateRequest.getNewBarcode(), existingProduct.getId());
        applyUpdate(existingProduct, updateRequest);

        return productDao.save(existingProduct);
    }

    @Override
    public List<ProductPojo> saveAll(List<ProductPojo> productsToSave) {
        return productDao.saveAll(productsToSave);
    }

    // -------------------- private helpers --------------------

    private ProductPojo loadProductByBarcode(String barcode) throws ApiException {
        ProductPojo product = productDao.findByBarcode(barcode);
        if (product == null) {
            throw new ApiException("Product not found");
        }
        return product;
    }

    private void ensureBarcodeIsUnique(String barcode, String allowedProductId) throws ApiException {
        ProductPojo existing = productDao.findByBarcode(barcode);
        if (existing == null) {
            return;
        }

        if (allowedProductId != null && allowedProductId.equals(existing.getId())) {
            return;
        }

        throw new ApiException("Duplicate barcode");
    }

    private void applyUpdate(ProductPojo existingProduct, ProductUpdatePojo updateRequest) {
        existingProduct.setBarcode(updateRequest.getNewBarcode());
        existingProduct.setClientEmail(updateRequest.getClientEmail());
        existingProduct.setName(updateRequest.getName());
        existingProduct.setMrp(updateRequest.getMrp());
        existingProduct.setImageUrl(updateRequest.getImageUrl());
    }
}
