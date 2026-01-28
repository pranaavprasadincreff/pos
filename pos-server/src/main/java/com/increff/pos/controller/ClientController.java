package com.increff.pos.controller;

import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.form.ClientForm;
import com.increff.pos.model.form.PageForm;
import com.increff.pos.dto.ClientDto;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.ClientUpdateForm;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;

@Tag(name = "Client Management", description = "APIs for managing clients")
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/client")
public class ClientController {
    private final ClientDto clientDto;
    public ClientController(ClientDto clientDto) {
        this.clientDto = clientDto;
    }

    @Operation(summary = "Create a new client")
    @RequestMapping(path = "/add", method = RequestMethod.POST)
    public ClientData createClient(@RequestBody ClientForm clientForm) throws ApiException {
        return clientDto.createClient(clientForm);
    }

    @Operation(summary = "Get all clients with pagination")
    @RequestMapping(path = "/get-all-paginated", method = RequestMethod.POST)
    public Page<ClientData> getAllClients(@RequestBody PageForm form) throws ApiException {
        return clientDto.getAllClients(form);
    }

    @Operation(summary = "Get client by email")
    @RequestMapping(path = "/get-by-email/{email}", method = RequestMethod.GET)
    public ClientData getByEmail(@PathVariable String email) throws ApiException {
        return clientDto.getByEmail(email);
    }

    @Operation(summary = "Update existing client")
    @RequestMapping(path = "/update", method = RequestMethod.PUT)
    public ClientData updateClient(@RequestBody ClientUpdateForm clientUpdateForm) throws ApiException {
        return clientDto.updateClient(clientUpdateForm);
    }
}
