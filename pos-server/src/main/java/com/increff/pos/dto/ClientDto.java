package com.increff.pos.dto;

import com.increff.pos.api.ClientApi;
import com.increff.pos.db.ClientPojo;
import com.increff.pos.helper.ClientHelper;
import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.ClientForm;
import com.increff.pos.model.form.ClientSearchForm;
import com.increff.pos.model.form.ClientUpdateForm;
import com.increff.pos.model.form.PageForm;
import com.increff.pos.util.NormalizationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ClientDto {
    @Autowired
    private ClientApi clientApi;

    public ClientData create(ClientForm clientCreateForm) throws ApiException {
        NormalizationUtil.normalizeClientForm(clientCreateForm);
        ClientPojo clientToCreate = ClientHelper.convertFormToEntity(clientCreateForm);
        ClientPojo createdClient = clientApi.add(clientToCreate);
        return ClientHelper.convertFormToDto(createdClient);
    }

    public ClientData getByEmail(String email) throws ApiException {
        String normalizedEmail = NormalizationUtil.normalizeEmail(email);
        ClientPojo client = clientApi.getClientByEmail(normalizedEmail);
        return ClientHelper.convertFormToDto(client);
    }

    public Page<ClientData> search(ClientSearchForm clientSearchForm) throws ApiException {
        NormalizationUtil.normalizeClientSearchForm(clientSearchForm);
        Page<ClientPojo> clientPage = clientApi.search(clientSearchForm);
        return clientPage.map(ClientHelper::convertFormToDto);
    }

    public ClientData update(ClientUpdateForm clientUpdateForm) throws ApiException {
        NormalizationUtil.normalizeClientUpdateForm(clientUpdateForm);
        String oldEmail = clientUpdateForm.getOldEmail();
        ClientPojo clientToUpdate = ClientHelper.convertUpdateFormToClientPojo(clientUpdateForm);
        ClientPojo updatedClient = clientApi.update(clientToUpdate, oldEmail);
        return ClientHelper.convertFormToDto(updatedClient);
    }

    public Page<ClientData> getAllUsingSearch(PageForm pageForm) throws ApiException {
        ClientSearchForm clientSearchForm = buildEmptySearchFromPage(pageForm);
        return search(clientSearchForm);
    }

    private ClientSearchForm buildEmptySearchFromPage(PageForm pageForm) {
        ClientSearchForm clientFilterForm = new ClientSearchForm();
        clientFilterForm.setPage(pageForm.getPage());
        clientFilterForm.setSize(pageForm.getSize());
        return clientFilterForm;
    }
}
