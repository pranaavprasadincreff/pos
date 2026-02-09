package com.increff.pos.api;

import com.increff.pos.db.ClientPojo;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.ClientSearchForm;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Set;

public interface ClientApi {
    ClientPojo add(ClientPojo clientPojo) throws ApiException;
    ClientPojo getClientByEmail(String email) throws ApiException;
    ClientPojo update(ClientPojo clientToUpdate, String oldEmail) throws ApiException;
    Page<ClientPojo> search(ClientSearchForm form);
    void validateClientsExist(Set<String> emails) throws ApiException;
    List<String> findClientEmailsByNameOrEmail(String query, int limit);
}
