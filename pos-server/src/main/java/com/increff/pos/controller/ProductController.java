package com.increff.pos.controller;

import com.increff.pos.dto.ProductDto;
import com.increff.pos.exception.ApiException;
import com.increff.pos.model.data.BulkUploadData;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.form.BulkUploadForm;
import com.increff.pos.model.form.PageForm;
import com.increff.pos.model.form.ProductForm;
import com.increff.pos.model.form.ProductUpdateForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Product Management", description = "APIs for managing products")
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/product")
public class ProductController {
    private final ProductDto productDto;

    public ProductController(ProductDto productDto) {
        this.productDto = productDto;
    }

    @Operation(summary = "Add a new product")
    @RequestMapping(path = "/add", method = RequestMethod.POST)
    public ProductData add(@RequestBody ProductForm form) throws ApiException {
        return productDto.addProduct(form);
    }

    @Operation(summary = "Get product by barcode")
    @RequestMapping(path = "/get-by-barcode/{barcode}", method = RequestMethod.GET)
    public ProductData getByBarcode(@PathVariable String barcode) throws ApiException {
        return productDto.getByBarcode(barcode);
    }

    @Operation(summary = "Get all products with pagination")
    @RequestMapping(path = "/get-all-paginated", method = RequestMethod.POST)
    public Page<ProductData> getAll(@RequestBody PageForm form) throws ApiException {
        return productDto.getAll(form);
    }

    @Operation(summary = "Update product details")
    @RequestMapping(path = "/update", method = RequestMethod.PUT)
    public ProductData update(@RequestBody ProductUpdateForm form) throws ApiException {
        return productDto.updateProduct(form);
    }

    @Operation(summary = "Bulk add products via TSV")
    @RequestMapping(path = "/bulk-add-products", method = RequestMethod.POST)
    public BulkUploadData bulkAddProducts(@RequestBody BulkUploadForm form) throws ApiException {
        return productDto.bulkAddProducts(form);
    }
}
