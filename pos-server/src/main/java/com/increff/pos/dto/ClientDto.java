package com.increff.pos.dto;

import com.increff.pos.api.ClientApi;
import com.increff.pos.db.ClientUpdatePojo;
import com.increff.pos.helper.ClientHelper;
import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.form.ClientForm;
import com.increff.pos.db.ClientPojo;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.PageForm;
import com.increff.pos.model.form.ClientUpdateForm;
import com.increff.pos.util.ValidationUtil;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ClientDto {
    private final ClientApi clientApi;
    public ClientDto(ClientApi clientApi) {
        this.clientApi = clientApi;
    }

    public ClientData createClient(ClientForm clientForm) throws ApiException {
        ValidationUtil.validateClientForm(clientForm);
        ClientPojo clientPojo = ClientHelper.convertFormToEntity(clientForm);
        ClientPojo savedClientPojo = clientApi.addClient(clientPojo);
        return ClientHelper.convertFormToDto(savedClientPojo);
    }

    public ClientData getById(String id) throws ApiException {
        ClientPojo clientPojo = clientApi.getClientById(id);
        return ClientHelper.convertFormToDto(clientPojo);
    }

    public ClientData getByEmail(String email) throws ApiException {
        ClientPojo clientPojo = clientApi.getClientByEmail(email);
        return ClientHelper.convertFormToDto(clientPojo);
    }

    public Page<ClientData> getAllClients(PageForm form) throws ApiException {
        ValidationUtil.validatePageForm(form);
        Page<ClientPojo> ClientPage = clientApi.getAllClients(form.getPage(), form.getSize());
        return ClientPage.map(ClientHelper::convertFormToDto);
    }

    public ClientData updateClient(ClientUpdateForm clientUpdateForm) throws ApiException {
        ValidationUtil.validateClientUpdateForm(clientUpdateForm);
        ClientUpdatePojo clientUpdatePojo = ClientHelper.convertUpdateFormToEntity(clientUpdateForm);
        ClientPojo updated = clientApi.updateClient(clientUpdatePojo);
        return ClientHelper.convertFormToDto(updated);
    }

} 