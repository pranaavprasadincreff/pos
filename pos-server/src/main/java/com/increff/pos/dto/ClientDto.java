package com.increff.pos.dto;

import com.increff.pos.api.ClientApi;
import com.increff.pos.db.ClientPojo;
import com.increff.pos.db.ClientUpdatePojo;
import com.increff.pos.helper.ClientHelper;
import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.ClientFilterForm;
import com.increff.pos.model.form.ClientForm;
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

    public Page<ClientData> filter(ClientFilterForm clientFilterForm) throws ApiException {
        NormalizationUtil.normalizeClientFilterForm(clientFilterForm);
        Page<ClientPojo> clientPage = clientApi.filter(
                clientFilterForm.getName(),
                clientFilterForm.getEmail(),
                clientFilterForm.getPage(),
                clientFilterForm.getSize()
        );
        return clientPage.map(ClientHelper::convertFormToDto);
    }

    public ClientData update(ClientUpdateForm clientUpdateForm) throws ApiException {
        NormalizationUtil.normalizeClientUpdateForm(clientUpdateForm);
        ClientUpdatePojo updateRequest = ClientHelper.convertUpdateFormToEntity(clientUpdateForm);
        ClientPojo updatedClient = clientApi.update(updateRequest);
        return ClientHelper.convertFormToDto(updatedClient);
    }

    public Page<ClientData> getAllUsingFilter(PageForm pageForm) throws ApiException {
        ClientFilterForm clientFilterForm = buildEmptyFilterFromPage(pageForm);
        return filter(clientFilterForm);
    }

    private ClientFilterForm buildEmptyFilterFromPage(PageForm pageForm) {
        ClientFilterForm clientFilterForm = new ClientFilterForm();
        clientFilterForm.setPage(pageForm.getPage());
        clientFilterForm.setSize(pageForm.getSize());
        return clientFilterForm;
    }
}
