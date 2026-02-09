package com.increff.pos.controller;

import com.increff.pos.dto.ClientDto;
import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.ClientForm;
import com.increff.pos.model.form.ClientSearchForm;
import com.increff.pos.model.form.ClientUpdateForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Client Management", description = "APIs for managing clients")
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/client")
public class ClientController {

    @Autowired
    private ClientDto clientDto;

    @Operation(summary = "Create a new client")
    @PostMapping
    public ClientData create(@RequestBody ClientForm clientForm) throws ApiException {
        return clientDto.create(clientForm);
    }

    @Operation(summary = "Get client by email")
    @GetMapping("/{email}")
    public ClientData getByEmail(@PathVariable String email) throws ApiException {
        return clientDto.getByEmail(email);
    }

    @Operation(summary = "Update existing client")
    @PutMapping
    public ClientData update(@RequestBody ClientUpdateForm form) throws ApiException {
        return clientDto.update(form);
    }

    @Operation(summary = "Search clients")
    @PostMapping("/search")
    public Page<ClientData> search(@RequestBody ClientSearchForm form) throws ApiException {
        return clientDto.search(form);
    }
}
