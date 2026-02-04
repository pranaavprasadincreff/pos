package com.increff.pos.model.form;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class BulkUploadForm {

    @NotBlank(message = "File content is required")
    private String file;
}
