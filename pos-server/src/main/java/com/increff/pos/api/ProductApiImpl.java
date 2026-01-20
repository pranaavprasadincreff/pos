package com.increff.pos.api;

import com.increff.pos.dao.ProductDao;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.db.ProductUpdatePojo;
import com.increff.pos.exception.ApiException;
import com.increff.pos.util.BarcodeNormalizer;
import com.increff.pos.util.EmailNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class ProductApiImpl implements ProductApi {
    private static final Logger logger = LoggerFactory.getLogger(ProductApiImpl.class);
    private final ProductDao dao;
    private final UserApi userApi;

    public ProductApiImpl(ProductDao dao, UserApi userApi) {
        this.dao = dao;
        this.userApi = userApi;
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public ProductPojo addProduct(ProductPojo productPojo) throws ApiException {
        logger.info("Creating product with barcode: {}", productPojo.getBarcode());
        productPojo.setBarcode(BarcodeNormalizer.normalize(productPojo.getBarcode()));
        productPojo.setClientEmail(EmailNormalizer.normalize((productPojo.getClientEmail())));
        checkIfBarcodeExists(productPojo);
        checkIfEmailExists(productPojo.getClientEmail());
        ProductPojo saved = dao.save(productPojo);
        logger.info("Created product with id: {}", saved.getId());
        return saved;
    }

    @Override
    public ProductPojo getProductById(String id) throws ApiException {
        ProductPojo product = dao.findById(id).orElse(null);
        if (Objects.isNull(product)) {
            throw new ApiException("Product not found with id: " + id);
        }
        return product;
    }

    @Override
    public ProductPojo getProductByBarcode(String barcode) throws ApiException {
        String normalized = BarcodeNormalizer.normalize(barcode);
        ProductPojo product = dao.findByBarcode(normalized);
        if (Objects.isNull(product)) {
            throw new ApiException("Product not found with barcode: " + barcode);
        }
        return product;
    }

    @Override
    public Page<ProductPojo> getAllProducts(int page, int size) {
        PageRequest pageRequest =
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return dao.findAll(pageRequest);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public ProductPojo updateProduct(ProductUpdatePojo updatePojo) throws ApiException {
        logger.info("Updating product with barcode: {}", updatePojo.getOldBarcode());
        updatePojo.setOldBarcode(BarcodeNormalizer.normalize(updatePojo.getOldBarcode()));
        updatePojo.setNewBarcode(BarcodeNormalizer.normalize(updatePojo.getNewBarcode()));
        updatePojo.setClientEmail(EmailNormalizer.normalize((updatePojo.getClientEmail())));
        ProductPojo existing = getProductByBarcode(updatePojo.getOldBarcode());
        checkIfBarcodeExistsForUpdate(updatePojo);
        checkIfEmailExists(updatePojo.getClientEmail());

        existing.setBarcode(updatePojo.getNewBarcode());
        existing.setClientEmail(updatePojo.getClientEmail());
        existing.setName(updatePojo.getName());
        existing.setMrp(updatePojo.getMrp());
        existing.setImageUrl(updatePojo.getImageUrl());
        return dao.save(existing);
    }

    private void checkIfBarcodeExists(ProductPojo productPojo) throws ApiException {
        ProductPojo existing = dao.findByBarcode(productPojo.getBarcode());
        if (existing != null) {
            throw new ApiException(
                    "Product already exists with barcode: " + productPojo.getBarcode()
            );
        }
    }

    private void checkIfBarcodeExistsForUpdate(ProductUpdatePojo updatePojo)
            throws ApiException {
        ProductPojo existingWithNewBarcode = dao.findByBarcode(updatePojo.getNewBarcode());
        ProductPojo existingWithOldBarcode = dao.findByBarcode(updatePojo.getOldBarcode());
        if (existingWithNewBarcode != null &&
                !existingWithNewBarcode.getId().equals(existingWithOldBarcode.getId())) {
            throw new ApiException(
                    "Product already exists with barcode: " + updatePojo.getNewBarcode()
            );
        }
    }

    private void checkIfEmailExists(String clientEmail) throws ApiException {
        try {
            userApi.getUserByEmail(clientEmail);
        } catch (ApiException e) {
            throw new ApiException("Client does not exist with email: " + clientEmail);
        }
    }
}
