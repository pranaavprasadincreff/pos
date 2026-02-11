package com.increff.pos.api;

import com.increff.pos.dao.ProductDao;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.ProductSearchForm;
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
        ProductPojo product = productDao.findByBarcode(barcode);
        if (product == null) {
            throw new ApiException("Product not found");
        }
        return product;
    }

    @Override
    public ProductPojo getById(String productId) throws ApiException {
        return loadProductById(productId);
    }

    @Override
    public Page<ProductPojo> search(ProductSearchForm form, List<String> clientEmails) {
        return productDao.search(form, clientEmails);
    }

    @Override
    public List<ProductPojo> findByBarcodes(List<String> barcodes) {
        return productDao.findByBarcodes(barcodes);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public ProductPojo updateProduct(ProductPojo productToUpdate) throws ApiException {
        ProductPojo existingProduct = loadProductById(productToUpdate.getId());
        ensureBarcodeIsUnique(productToUpdate.getBarcode(), existingProduct.getId());
        return productDao.save(productToUpdate);
    }

    @Override
    public List<ProductPojo> saveAll(List<ProductPojo> productsToSave) {
        return productDao.saveAll(productsToSave);
    }

    @Override
    public List<ProductPojo> findByIds(List<String> productIds) {
        return productDao.findByIds(productIds);
    }

    // -------------------- private helpers --------------------

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

    private ProductPojo loadProductById(String productId) throws ApiException {
        if (productId == null || productId.isBlank()) {
            throw new ApiException("Product not found");
        }

        return productDao.findById(productId)
                .orElseThrow(() -> new ApiException("Product not found"));
    }
}
