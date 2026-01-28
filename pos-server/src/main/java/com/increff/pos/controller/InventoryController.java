package com.increff.pos.controller;

import com.increff.pos.dto.ProductDto;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.data.BulkUploadData;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.form.BulkUploadForm;
import com.increff.pos.model.form.InventoryUpdateForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Inventory Management", description = "APIs for managing inventory")
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final ProductDto productDto;

    public InventoryController(ProductDto productDto) {
        this.productDto = productDto;
    }

    @Operation(summary = "Update inventory for a product")
    @RequestMapping(path = "/update", method = RequestMethod.PATCH)
    public ProductData updateInventory(@RequestBody InventoryUpdateForm form) throws ApiException {
        return productDto.updateInventory(form);
    }

    @Operation(summary = "Bulk update inventory via TSV")
    @RequestMapping(path = "/bulk-inventory-update", method = RequestMethod.POST)
    public BulkUploadData bulkInventoryUpdate(@RequestBody BulkUploadForm form)
            throws ApiException {
        return productDto.bulkUpdateInventory(form);
    }
}
