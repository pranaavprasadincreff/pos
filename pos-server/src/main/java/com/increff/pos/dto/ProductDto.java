package com.increff.pos.dto;

import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.InventoryUpdatePojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.db.ProductUpdatePojo;
import com.increff.pos.flow.ProductFlow;
import com.increff.pos.helper.BulkUploadHelper;
import com.increff.pos.helper.ProductHelper;
import com.increff.pos.helper.TsvHelper;
import com.increff.pos.model.data.BulkUploadData;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.*;
import com.increff.pos.util.NormalizationUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProductDto {
    @Autowired
    private ProductFlow productFlow;

    public ProductData addProduct(ProductForm form) throws ApiException {
        NormalizationUtil.normalizeProductForm(form);

        ProductPojo productToCreate = ProductHelper.convertProductFormToEntity(form);
        Pair<ProductPojo, InventoryPojo> created = productFlow.addProduct(productToCreate);

        return ProductHelper.convertToProductData(created.getLeft(), created.getRight());
    }

    public ProductData getByBarcode(String barcode) throws ApiException {
        String normalizedBarcode = NormalizationUtil.normalizeBarcode(barcode);
        Pair<ProductPojo, InventoryPojo> pair = productFlow.getByBarcode(normalizedBarcode);
        return ProductHelper.convertToProductData(pair.getLeft(), pair.getRight());
    }

    public Page<ProductData> getAllUsingFilter(PageForm form) throws ApiException {
        return getAllProducts(form);
    }

    public Page<ProductData> filter(ProductFilterForm form) throws ApiException {
        NormalizationUtil.normalizeProductFilterForm(form);

        Page<Pair<ProductPojo, InventoryPojo>> page = productFlow.filter(form);
        return convertToProductDataPage(page);
    }

    public ProductData updateProduct(ProductUpdateForm form) throws ApiException {
        NormalizationUtil.normalizeProductUpdateForm(form);

        ProductUpdatePojo update = ProductHelper.convertProductUpdateFormToEntity(form);
        Pair<ProductPojo, InventoryPojo> pair = productFlow.updateProduct(update);

        return ProductHelper.convertToProductData(pair.getLeft(), pair.getRight());
    }

    public ProductData updateInventory(InventoryUpdateForm form) throws ApiException {
        NormalizationUtil.normalizeInventoryUpdateForm(form);

        InventoryUpdatePojo update = ProductHelper.convertInventoryUpdateFormToEntity(form);
        Pair<ProductPojo, InventoryPojo> pair = productFlow.updateInventory(update);

        return ProductHelper.convertToProductData(pair.getLeft(), pair.getRight());
    }

    public BulkUploadData bulkAddProducts(BulkUploadForm form) throws ApiException {
        ParsedBulkFile parsedFile = parseProductBulkFile(form);

        List<ProductPojo> alignedProducts = new ArrayList<>(parsedFile.rows().size());
        Map<Integer, String> errorByRowIndex = new HashMap<>();

        for (int rowIndex = 0; rowIndex < parsedFile.rows().size(); rowIndex++) {
            try {
                alignedProducts.add(parseBulkProductRow(parsedFile.rows().get(rowIndex), parsedFile.headers()));
            } catch (ApiException exception) {
                alignedProducts.add(null);
                errorByRowIndex.put(rowIndex, exception.getMessage());
            }
        }

        List<String[]> flowResults = productFlow.bulkAddProducts(alignedProducts);
        BulkUploadHelper.applyRowErrors(flowResults, errorByRowIndex);

        return new BulkUploadData(TsvHelper.encodeResult(flowResults));
    }

    public BulkUploadData bulkUpdateInventory(BulkUploadForm form) throws ApiException {
        ParsedBulkFile parsedFile = parseInventoryBulkFile(form);

        List<InventoryPojo> alignedInventoryDeltas = new ArrayList<>(parsedFile.rows().size());
        Map<Integer, String> errorByRowIndex = new HashMap<>();

        for (int rowIndex = 0; rowIndex < parsedFile.rows().size(); rowIndex++) {
            try {
                alignedInventoryDeltas.add(parseBulkInventoryRow(parsedFile.rows().get(rowIndex), parsedFile.headers()));
            } catch (ApiException exception) {
                alignedInventoryDeltas.add(null);
                errorByRowIndex.put(rowIndex, exception.getMessage());
            }
        }

        List<String[]> flowResults = productFlow.bulkUpdateInventory(alignedInventoryDeltas);
        BulkUploadHelper.applyRowErrors(flowResults, errorByRowIndex);

        return new BulkUploadData(TsvHelper.encodeResult(flowResults));
    }

    // -------------------- private helpers --------------------

    private Page<ProductData> getAllProducts(PageForm form) throws ApiException {
        ProductFilterForm filterForm = new ProductFilterForm();
        filterForm.setBarcode(null);
        filterForm.setName(null);
        filterForm.setClient(null);
        filterForm.setPage(form.getPage());
        filterForm.setSize(form.getSize());

        return filter(filterForm);
    }

    private ParsedBulkFile parseProductBulkFile(BulkUploadForm form) throws ApiException {
        Pair<Map<String, Integer>, List<String[]>> parsed = BulkUploadHelper.parseProductFile(form.getFile());
        return new ParsedBulkFile(parsed.getLeft(), parsed.getRight());
    }

    private ParsedBulkFile parseInventoryBulkFile(BulkUploadForm form) throws ApiException {
        Pair<Map<String, Integer>, List<String[]>> parsed = BulkUploadHelper.parseInventoryFile(form.getFile());
        return new ParsedBulkFile(parsed.getLeft(), parsed.getRight());
    }

    private ProductPojo parseBulkProductRow(String[] row, Map<String, Integer> headers) throws ApiException {
        String barcode = BulkUploadHelper.normalizeBarcode(BulkUploadHelper.readCell(row, headers, "barcode"));
        String clientEmail = BulkUploadHelper.normalizeEmail(BulkUploadHelper.readCell(row, headers, "clientemail"));
        String productName = BulkUploadHelper.normalizeName(BulkUploadHelper.readCell(row, headers, "name"));

        Double mrp = BulkUploadHelper.parseMrp(BulkUploadHelper.readCell(row, headers, "mrp"));

        String imageUrl = null;
        if (headers.containsKey("imageurl")) {
            imageUrl = BulkUploadHelper.normalizeUrl(BulkUploadHelper.readCell(row, headers, "imageurl"));
        }

        BulkUploadHelper.validateProductRow(barcode, clientEmail, productName, mrp, imageUrl);

        return ProductHelper.createProductPojo(barcode, clientEmail, productName, mrp, imageUrl);
    }

    private InventoryPojo parseBulkInventoryRow(String[] row, Map<String, Integer> headers) throws ApiException {
        String barcode = BulkUploadHelper.normalizeBarcode(BulkUploadHelper.readCell(row, headers, "barcode"));

        Integer delta = BulkUploadHelper.parseInventoryDelta(BulkUploadHelper.readCell(row, headers, "inventory"));
        BulkUploadHelper.validateInventoryRow(barcode, delta);

        return ProductHelper.createInventoryDeltaPojo(barcode, delta);
    }

    private Page<ProductData> convertToProductDataPage(Page<Pair<ProductPojo, InventoryPojo>> page) {
        List<ProductData> data = page.getContent()
                .stream()
                .map(pair -> ProductHelper.convertToProductData(pair.getLeft(), pair.getRight()))
                .toList();

        return new PageImpl<>(data, page.getPageable(), page.getTotalElements());
    }

    private record ParsedBulkFile(Map<String, Integer> headers, List<String[]> rows) {
    }
}
