package com.increff.pos.controller;

import com.increff.pos.dto.ProductDto;
import com.increff.pos.model.data.BulkUploadData;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.BulkUploadForm;
import com.increff.pos.model.form.ProductForm;
import com.increff.pos.model.form.ProductSearchForm;
import com.increff.pos.model.form.ProductUpdateForm;
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
    @PostMapping
    public ProductData add(@RequestBody ProductForm form) throws ApiException {
        return productDto.addProduct(form);
    }

    @Operation(summary = "Get by barcode")
    @GetMapping("/{barcode}")
    public ProductData getByBarcode(@PathVariable String barcode) throws ApiException {
        return productDto.getByBarcode(barcode);
    }

    @Operation(summary = "Search products")
    @PostMapping("/search")
    public Page<ProductData> search(@RequestBody ProductSearchForm form) throws ApiException {
        return productDto.search(form);
    }

    @Operation(summary = "Update product")
    @PutMapping
    public ProductData update(@RequestBody ProductUpdateForm form) throws ApiException {
        return productDto.updateProduct(form);
    }

    @Operation(summary = "Bulk add")
    @PostMapping("/bulk-add-products")
    public BulkUploadData bulkAdd(@RequestBody BulkUploadForm form) throws ApiException {
        return productDto.bulkAddProducts(form);
    }
}
