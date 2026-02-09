package com.increff.pos.controller;

import com.increff.pos.dto.ProductDto;
import com.increff.pos.model.data.BulkUploadData;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.BulkUploadForm;
import com.increff.pos.model.form.InventoryUpdateForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Inventory Management")
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private ProductDto productDto;

    @Operation(summary = "Update inventory")
    @PutMapping
    public ProductData updateInventory(@RequestBody InventoryUpdateForm form) throws ApiException {
        return productDto.updateInventory(form);
    }

    @Operation(summary = "Bulk inventory update")
    @PostMapping("/bulk-inventory-update")
    public BulkUploadData bulkInventory(@RequestBody BulkUploadForm form) throws ApiException {
        return productDto.bulkUpdateInventory(form);
    }
}
