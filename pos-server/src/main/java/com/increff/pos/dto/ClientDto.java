package com.increff.pos.dto;

import com.increff.pos.api.ClientApi;
import com.increff.pos.db.ClientPojo;
import com.increff.pos.helper.ClientHelper;
import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.ClientForm;
import com.increff.pos.model.form.ClientSearchForm;
import com.increff.pos.model.form.ClientUpdateForm;
import com.increff.pos.util.FormValidationUtil;
import com.increff.pos.util.NormalizationUtil;
import com.increff.pos.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ClientDto {

    @Autowired
    private ClientApi clientApi;

    public ClientData create(ClientForm clientCreateForm) throws ApiException {
        NormalizationUtil.normalizeClientForm(clientCreateForm);
        FormValidationUtil.validate(clientCreateForm);

        getCheckEmailIsUnique(clientCreateForm.getEmail());
        ClientPojo clientToCreate = ClientHelper.convertFormToEntity(clientCreateForm);
        ClientPojo createdClient = clientApi.add(clientToCreate);
        return ClientHelper.convertFormToData(createdClient);
    }

    public ClientData getByEmail(String email) throws ApiException {
        String normalizedEmail = NormalizationUtil.normalizeEmail(email);
        ValidationUtil.validateEmail(normalizedEmail);

        ClientPojo client = clientApi.getClientByEmail(normalizedEmail);
        return ClientHelper.convertFormToData(client);
    }

    public Page<ClientData> search(ClientSearchForm clientSearchForm) throws ApiException {
        NormalizationUtil.normalizeClientSearchForm(clientSearchForm);
        FormValidationUtil.validate(clientSearchForm);

        Page<ClientPojo> clientPage = clientApi.search(clientSearchForm);
        return clientPage.map(ClientHelper::convertFormToData);
    }

    public ClientData update(ClientUpdateForm clientUpdateForm) throws ApiException {
        NormalizationUtil.normalizeClientUpdateForm(clientUpdateForm);
        FormValidationUtil.validate(clientUpdateForm);

        ClientPojo existingClient = fetchAndValidateClientToUpdate(clientUpdateForm);
        ClientPojo clientToUpdate =
                ClientHelper.convertToUpdatedPojo(existingClient, clientUpdateForm);
        ClientPojo updatedClient = clientApi.update(clientToUpdate);
        return ClientHelper.convertFormToData(updatedClient);
    }

    private void getCheckEmailIsUnique(String email) throws ApiException {
        try {
            clientApi.getClientByEmail(email);
            throw new ApiException("Client already exists with email: " + email);
        } catch (ApiException e) {
            if (!"CLIENT_NOT_FOUND".equals(e.getCode())) throw e;
        }
    }

    private ClientPojo fetchAndValidateClientToUpdate(ClientUpdateForm form) throws ApiException {
        ClientPojo existingClient = clientApi.getClientByEmail(form.getOldEmail());

        String newEmail = form.getNewEmail();
        if (!newEmail.equals(existingClient.getEmail())) {
            getCheckEmailIsUnique(newEmail);
        }
        return existingClient;
    }
}
