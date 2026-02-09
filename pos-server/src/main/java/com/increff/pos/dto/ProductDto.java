package com.increff.pos.dto;

import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.ProductPojo;
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

    public Page<ProductData> getAllUsingSearch(PageForm form) throws ApiException {
        return getAllProducts(form);
    }

    public Page<ProductData> search(ProductSearchForm form) throws ApiException {
        NormalizationUtil.normalizeProductSearchForm(form);
        Page<Pair<ProductPojo, InventoryPojo>> page = productFlow.search(form);
        return ProductHelper.convertToProductDataPage(page);
    }

    public ProductData updateProduct(ProductUpdateForm form) throws ApiException {
        NormalizationUtil.normalizeProductUpdateForm(form);

        String oldBarcode = form.getOldBarcode();
        ProductPojo productToUpdate = ProductHelper.convertProductUpdateFormToProductPojo(form);

        Pair<ProductPojo, InventoryPojo> pair = productFlow.updateProduct(productToUpdate, oldBarcode);
        return ProductHelper.convertToProductData(pair.getLeft(), pair.getRight());
    }

    public ProductData updateInventory(InventoryUpdateForm form) throws ApiException {
        NormalizationUtil.normalizeInventoryUpdateForm(form);

        String barcode = form.getBarcode();
        InventoryPojo inventoryToUpdate = ProductHelper.convertInventoryUpdateFormToInventoryPojo(form);

        Pair<ProductPojo, InventoryPojo> pair = productFlow.updateInventory(inventoryToUpdate, barcode);
        return ProductHelper.convertToProductData(pair.getLeft(), pair.getRight());
    }

    public BulkUploadData bulkAddProducts(BulkUploadForm form) throws ApiException {
        ParsedBulkFile parsedFile = parseProductBulkFile(form);

        List<ProductPojo> alignedProducts = new ArrayList<>(parsedFile.rows().size());
        Map<Integer, String> errorByRowIndex = new HashMap<>();
        List<String> barcodesByRow = new ArrayList<>(parsedFile.rows().size());

        for (int rowIndex = 0; rowIndex < parsedFile.rows().size(); rowIndex++) {
            String[] row = parsedFile.rows().get(rowIndex);

            String rawBarcode = BulkUploadHelper.readCell(row, parsedFile.headers(), "barcode");
            String normalizedBarcode = BulkUploadHelper.normalizeBarcode(rawBarcode);
            barcodesByRow.add(normalizedBarcode);

            try {
                alignedProducts.add(parseBulkProductRow(row, parsedFile.headers()));
            } catch (ApiException exception) {
                alignedProducts.add(null);
                errorByRowIndex.put(rowIndex, exception.getMessage());
            }
        }

        List<String[]> flowResults = productFlow.bulkAddProducts(alignedProducts);

        forceBarcodeInResults(flowResults, barcodesByRow);

        BulkUploadHelper.applyRowErrors(flowResults, errorByRowIndex);
        return new BulkUploadData(TsvHelper.encodeResult(flowResults));
    }


    public BulkUploadData bulkUpdateInventory(BulkUploadForm form) throws ApiException {
        ParsedBulkFile parsedFile = parseInventoryBulkFile(form);

        List<InventoryPojo> alignedInventoryDeltas = new ArrayList<>(parsedFile.rows().size());
        Map<Integer, String> errorByRowIndex = new HashMap<>();
        List<String> barcodesByRow = new ArrayList<>(parsedFile.rows().size());

        for (int rowIndex = 0; rowIndex < parsedFile.rows().size(); rowIndex++) {
            String[] row = parsedFile.rows().get(rowIndex);

            String rawBarcode = BulkUploadHelper.readCell(row, parsedFile.headers(), "barcode");
            String normalizedBarcode = BulkUploadHelper.normalizeBarcode(rawBarcode);
            barcodesByRow.add(normalizedBarcode);

            try {
                alignedInventoryDeltas.add(parseBulkInventoryRow(row, parsedFile.headers()));
            } catch (ApiException exception) {
                alignedInventoryDeltas.add(null);
                errorByRowIndex.put(rowIndex, exception.getMessage());
            }
        }

        List<String[]> flowResults = productFlow.bulkUpdateInventory(alignedInventoryDeltas);

        forceBarcodeInResults(flowResults, barcodesByRow);

        BulkUploadHelper.applyRowErrors(flowResults, errorByRowIndex);
        return new BulkUploadData(TsvHelper.encodeResult(flowResults));
    }


    // -------------------- private helpers --------------------

    private Page<ProductData> getAllProducts(PageForm form) throws ApiException {
        ProductSearchForm searchFrom = new ProductSearchForm();
        searchFrom.setBarcode(null);
        searchFrom.setName(null);
        searchFrom.setClient(null);
        searchFrom.setPage(form.getPage());
        searchFrom.setSize(form.getSize());

        return search(searchFrom);
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

    private void forceBarcodeInResults(List<String[]> flowResults, List<String> barcodesByRow) {
        for (int i = 0; i < flowResults.size() && i < barcodesByRow.size(); i++) {
            String[] r = flowResults.get(i);
            if (r == null || r.length < 3) continue;
            r[0] = barcodesByRow.get(i) == null ? "" : barcodesByRow.get(i);
        }
    }

    private record ParsedBulkFile(Map<String, Integer> headers, List<String[]> rows) {
    }
}
