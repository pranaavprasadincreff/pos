package com.increff.pos.controller;

import com.increff.pos.dto.ProductDto;
import com.increff.pos.model.data.BulkUploadData;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Product Management")
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/product")
public class ProductController {
    @Autowired
    private ProductDto productDto;

    @Operation(summary = "Add product")
    @PostMapping("/add")
    public ProductData add(@RequestBody ProductForm form) throws ApiException {
        return productDto.addProduct(form);
    }

    @Operation(summary = "Get by barcode")
    @GetMapping("/get-by-barcode/{barcode}")
    public ProductData getByBarcode(@PathVariable String barcode) throws ApiException {
        return productDto.getByBarcode(barcode);
    }

    @Operation(summary = "Get paginated")
    @PostMapping("/get-all-paginated")
    public Page<ProductData> getAll(@RequestBody PageForm form) throws ApiException {
        return productDto.getAll(form);
    }

    @Operation(summary = "Filter products")
    @PostMapping("/filter")
    public Page<ProductData> filter(@RequestBody ProductFilterForm form) throws ApiException {
        return productDto.filter(form);
    }

    @Operation(summary = "Update product")
    @PutMapping("/update")
    public ProductData update(@RequestBody ProductUpdateForm form) throws ApiException {
        return productDto.updateProduct(form);
    }

    @Operation(summary = "Bulk add")
    @PostMapping("/bulk-add-products")
    public BulkUploadData bulkAdd(@RequestBody BulkUploadForm form) throws ApiException {
        return productDto.bulkAddProducts(form);
    }

    @Operation(summary = "Bulk inventory update")
    @PostMapping("/bulk-update-inventory")
    public BulkUploadData bulkInventory(@RequestBody BulkUploadForm form) throws ApiException {
        return productDto.bulkUpdateInventory(form);
    }
}
