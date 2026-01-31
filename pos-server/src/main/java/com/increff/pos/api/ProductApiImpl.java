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
    private ProductDao dao;

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public ProductPojo addProduct(ProductPojo product) throws ApiException {
        validateNewProduct(product);
        return dao.save(product);
    }

    @Override
    public ProductPojo getProductById(String id) throws ApiException {
        return getExistingById(id);
    }

    @Override
    public ProductPojo getProductByBarcode(String barcode) throws ApiException {
        return getExistingByBarcode(barcode);
    }

    @Override
    public Page<ProductPojo> getAllProducts(int page, int size) {
        return dao.findAll(
                PageRequest.of(page, size,
                        Sort.by("createdAt").descending()));
    }

    @Override
    public Page<ProductPojo> filter(ProductFilterForm form, List<String> clientEmails) {
        return dao.filter(form, clientEmails);
    }

    @Override
    public List<ProductPojo> findByBarcodes(List<String> barcodes) {
        return dao.findByBarcodes(barcodes);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public ProductPojo updateProduct(ProductUpdatePojo update) throws ApiException {

        ProductPojo existing = getExistingByBarcode(update.getOldBarcode());

        validateBarcodeChange(update, existing);
        applyUpdate(existing, update);

        return dao.save(existing);
    }

    @Override
    public List<ProductPojo> saveAll(List<ProductPojo> list) {
        return dao.saveAll(list);
    }

    private ProductPojo getExistingById(String id) throws ApiException {
        ProductPojo pojo = dao.findById(id).orElse(null);
        if (pojo == null) throw new ApiException("Product not found");
        return pojo;
    }

    private ProductPojo getExistingByBarcode(String barcode) throws ApiException {
        ProductPojo pojo = dao.findByBarcode(barcode);
        if (pojo == null) throw new ApiException("Product not found");
        return pojo;
    }

    private void validateNewProduct(ProductPojo p) throws ApiException {
        ensureBarcodeUnique(p.getBarcode(), null);
    }

    private void validateBarcodeChange(ProductUpdatePojo u,
                                       ProductPojo existing) throws ApiException {
        ensureBarcodeUnique(u.getNewBarcode(), existing.getId());
    }

    private void ensureBarcodeUnique(String barcode,
                                     String allowedProductId) throws ApiException {

        ProductPojo found = dao.findByBarcode(barcode);
        if (found == null) return;
        if (allowedProductId != null &&
                found.getId().equals(allowedProductId)) {
            return;
        }
        throw new ApiException("Duplicate barcode");
    }

    private void applyUpdate(ProductPojo e, ProductUpdatePojo u) {
        e.setBarcode(u.getNewBarcode());
        e.setClientEmail(u.getClientEmail());
        e.setName(u.getName());
        e.setMrp(u.getMrp());
        e.setImageUrl(u.getImageUrl());
    }
}
