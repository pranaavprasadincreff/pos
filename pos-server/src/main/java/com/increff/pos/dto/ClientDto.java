package com.increff.pos.dto;

import com.increff.pos.api.ClientApi;
import com.increff.pos.db.ClientPojo;
import com.increff.pos.helper.ClientHelper;
import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.ClientForm;
import com.increff.pos.model.form.ClientSearchForm;
import com.increff.pos.model.form.ClientUpdateForm;
import com.increff.pos.util.FormValidator;
import com.increff.pos.util.NormalizationUtil;
import com.increff.pos.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ClientDto {

    @Autowired
    private ClientApi clientApi;

    @Autowired
    private FormValidator formValidator;

    public ClientData create(ClientForm clientCreateForm) throws ApiException {
        NormalizationUtil.normalizeClientForm(clientCreateForm);
        formValidator.validate(clientCreateForm);

        ClientPojo clientToCreate = ClientHelper.convertFormToEntity(clientCreateForm);
        ClientPojo createdClient = clientApi.add(clientToCreate);
        return ClientHelper.convertFormToDto(createdClient);
    }

    public ClientData getByEmail(String email) throws ApiException {
        String normalizedEmail = NormalizationUtil.normalizeEmail(email);
        ValidationUtil.validateEmail(normalizedEmail);

        ClientPojo client = clientApi.getClientByEmail(normalizedEmail);
        return ClientHelper.convertFormToDto(client);
    }

    public Page<ClientData> search(ClientSearchForm clientSearchForm) throws ApiException {
        NormalizationUtil.normalizeClientSearchForm(clientSearchForm);
        formValidator.validate(clientSearchForm);

        Page<ClientPojo> clientPage = clientApi.search(clientSearchForm);
        return clientPage.map(ClientHelper::convertFormToDto);
    }

    public ClientData update(ClientUpdateForm clientUpdateForm) throws ApiException {
        NormalizationUtil.normalizeClientUpdateForm(clientUpdateForm);
        formValidator.validate(clientUpdateForm);

        String oldEmail = clientUpdateForm.getOldEmail();
        ClientPojo clientToUpdate = ClientHelper.convertUpdateFormToClientPojo(clientUpdateForm);
        ClientPojo updatedClient = clientApi.update(clientToUpdate, oldEmail);
        return ClientHelper.convertFormToDto(updatedClient);
    }
}
