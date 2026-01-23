package com.increff.pos.api;

import com.increff.pos.dao.ProductDao;
import com.increff.pos.db.InventoryPojo;
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
    private final InventoryApi inventoryApi;

    public ProductApiImpl(ProductDao dao, UserApi userApi, InventoryApi inventoryApi) {
        this.dao = dao;
        this.userApi = userApi;
        this.inventoryApi = inventoryApi;
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public ProductPojo addProduct(ProductPojo product) throws ApiException {
        logger.info("Creating product with barcode: {}", product.getBarcode());
        normalizeProduct(product);
        validateNewProduct(product);
        ProductPojo savedProduct = dao.save(product);
        createDefaultInventory(savedProduct);
        return savedProduct;
    }

    @Override
    public ProductPojo getProductById(String id) throws ApiException {
        return dao.findById(id)
                .orElseThrow(() ->
                        new ApiException("Product not found with id: " + id)
                );
    }

    @Override
    public ProductPojo getProductByBarcode(String barcode) throws ApiException {
        String normalized = BarcodeNormalizer.normalize(barcode);
        ProductPojo product = dao.findByBarcode(normalized);
        if (product == null) {
            throw new ApiException(
                    "Product not found with barcode: " + barcode
            );
        }
        return product;
    }

    @Override
    public Page<ProductPojo> getAllProducts(int page, int size) {
        return dao.findAll(buildPageRequest(page, size));
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public ProductPojo updateProduct(ProductUpdatePojo update) throws ApiException {
        logger.info("Updating product with barcode: {}", update.getOldBarcode());
        normalizeUpdate(update);
        ProductPojo existing = getProductByBarcode(update.getOldBarcode());
        validateUpdate(update, existing);
        applyUpdate(existing, update);
        return dao.save(existing);
    }

    private void normalizeProduct(ProductPojo product) {
        product.setBarcode(BarcodeNormalizer.normalize(product.getBarcode()));
        product.setClientEmail(EmailNormalizer.normalize(product.getClientEmail())
        );
    }

    private void normalizeUpdate(ProductUpdatePojo update) {
        update.setOldBarcode(BarcodeNormalizer.normalize(update.getOldBarcode()));
        update.setNewBarcode(BarcodeNormalizer.normalize(update.getNewBarcode()));
        update.setClientEmail(EmailNormalizer.normalize(update.getClientEmail()));
    }

    private void validateNewProduct(ProductPojo product) throws ApiException {
        ensureBarcodeDoesNotExist(product.getBarcode());
        ensureClientExists(product.getClientEmail());
    }

    private void validateUpdate(ProductUpdatePojo update, ProductPojo existing) throws ApiException {
        ensureNewBarcodeIsValid(update, existing);
        ensureClientExists(update.getClientEmail());
    }

    private void ensureBarcodeDoesNotExist(String barcode) throws ApiException {
        if (dao.findByBarcode(barcode) != null) {
            throw new ApiException("Product already exists with barcode: " + barcode);
        }
    }

    private void ensureNewBarcodeIsValid(ProductUpdatePojo update, ProductPojo existing) throws ApiException {
        ProductPojo withNewBarcode = dao.findByBarcode(update.getNewBarcode());
        if (withNewBarcode != null && !withNewBarcode.getId().equals(existing.getId())) {
            throw new ApiException("Product already exists with barcode: " + update.getNewBarcode()
            );
        }
    }

    private void ensureClientExists(String clientEmail) throws ApiException {
        try {
            userApi.getUserByEmail(clientEmail);
        } catch (ApiException e) {
            throw new ApiException("Client does not exist with email: " + clientEmail
            );
        }
    }

    private void createDefaultInventory(ProductPojo product) throws ApiException {
        InventoryPojo inventory = new InventoryPojo();
        inventory.setProductId(product.getId());
        inventory.setQuantity(0);
        inventoryApi.addInventory(inventory);
    }

    private void applyUpdate(ProductPojo existing, ProductUpdatePojo update) {
        existing.setBarcode(update.getNewBarcode());
        existing.setClientEmail(update.getClientEmail());
        existing.setName(update.getName());
        existing.setMrp(update.getMrp());
        existing.setImageUrl(update.getImageUrl());
    }

    private PageRequest buildPageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }
}