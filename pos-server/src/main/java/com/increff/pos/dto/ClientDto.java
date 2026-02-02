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
import com.increff.pos.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ClientDto {
    @Autowired
    private ClientApi clientApi;

    public ClientData create(ClientForm form) throws ApiException {
        normalizeClientCreate(form);
        validateClientCreate(form);

        ClientPojo pojo = ClientHelper.convertFormToEntity(form);
        ClientPojo saved = clientApi.add(pojo);
        return ClientHelper.convertFormToDto(saved);
    }

    public ClientData getByEmail(String email) throws ApiException {
        String normalized = normalizeAndValidateEmail(email);
        ClientPojo pojo = clientApi.getClientByEmail(normalized);
        return ClientHelper.convertFormToDto(pojo);
    }

    public Page<ClientData> getAllClients(PageForm form) throws ApiException {
        ValidationUtil.validatePageForm(form);
        Page<ClientPojo> page = clientApi.getAll(form.getPage(), form.getSize());
        return page.map(ClientHelper::convertFormToDto);
    }

    public Page<ClientData> filter(ClientFilterForm form) throws ApiException {
        normalizeClientFilter(form);
        ValidationUtil.validateClientFilterForm(form);

        Page<ClientPojo> page = clientApi.filter(
                form.getName(),
                form.getEmail(),
                form.getPage(),
                form.getSize()
        );
        return page.map(ClientHelper::convertFormToDto);
    }

    public ClientData update(ClientUpdateForm form) throws ApiException {
        normalizeClientUpdate(form);
        ValidationUtil.validateClientUpdateForm(form);

        ClientUpdatePojo pojo = ClientHelper.convertUpdateFormToEntity(form);
        ClientPojo updated = clientApi.update(pojo);
        return ClientHelper.convertFormToDto(updated);
    }

    private void normalizeClientCreate(ClientForm form) {
        NormalizationUtil.normalizeClientForm(form);
    }

    private void validateClientCreate(ClientForm form) throws ApiException {
        ValidationUtil.validateClientForm(form);
    }

    private void normalizeClientUpdate(ClientUpdateForm form) {
        NormalizationUtil.normalizeClientUpdateForm(form);
    }

    private void normalizeClientFilter(ClientFilterForm form) {
        NormalizationUtil.normalizeClientFilterForm(form);
    }

    private String normalizeAndValidateEmail(String email) throws ApiException {
        String normalized = NormalizationUtil.normalizeEmail(email);
        ValidationUtil.validateEmail(normalized);
        return normalized;
    }
}
