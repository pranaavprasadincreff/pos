package com.increff.pos.controller;

import com.increff.pos.dto.ClientDto;
import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.ClientFilterForm;
import com.increff.pos.model.form.ClientForm;
import com.increff.pos.model.form.ClientUpdateForm;
import com.increff.pos.model.form.PageForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Tag(name = "Client Management", description = "APIs for managing clients")
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/client")
@Validated
public class ClientController {
    @Autowired
    private ClientDto clientDto;

    @Operation(summary = "Create a new client")
    @RequestMapping(path = "/add", method = RequestMethod.POST)
    public ClientData create(@Valid @RequestBody ClientForm clientForm) throws ApiException {
        return clientDto.create(clientForm);
    }

    @Operation(summary = "Get all clients with pagination")
    @RequestMapping(path = "/get-all-paginated", method = RequestMethod.POST)
    public Page<ClientData> getAllClients(@Valid @RequestBody PageForm pageForm) throws ApiException {
        return clientDto.getAllUsingFilter(pageForm);
    }

    @Operation(summary = "Get client by email")
    @RequestMapping(path = "/get-by-email/{email}", method = RequestMethod.GET)
    public ClientData getByEmail(
            @PathVariable
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            @Size(max = 40, message = "Email too long")
            String email
    ) throws ApiException {
        return clientDto.getByEmail(email);
    }

    @Operation(summary = "Update existing client")
    @RequestMapping(path = "/update", method = RequestMethod.PUT)
    public ClientData updateClient(@Valid @RequestBody ClientUpdateForm form) throws ApiException {
        return clientDto.update(form);
    }

    @Operation(summary = "Filter clients")
    @RequestMapping(path = "/filter", method = RequestMethod.POST)
    public Page<ClientData> filter(@Valid @RequestBody ClientFilterForm form) throws ApiException {
        return clientDto.filter(form);
    }
}
