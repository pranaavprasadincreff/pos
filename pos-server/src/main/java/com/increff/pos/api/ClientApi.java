package com.increff.pos.api;

import com.increff.pos.db.ClientPojo;
import com.increff.pos.db.ClientUpdatePojo;
import com.increff.pos.model.exception.ApiException;
import org.springframework.data.domain.Page;

import java.util.Set;

public interface ClientApi {
    ClientPojo add(ClientPojo clientPojo) throws ApiException;
    ClientPojo getClientByEmail(String email) throws ApiException;
    Page<ClientPojo> getAll(int page, int size);
    ClientPojo update(ClientUpdatePojo clientUpdatePojo) throws ApiException;
    Page<ClientPojo> filter(String name, String email, int page, int size);
    void validateClientsExist(Set<String> emails) throws ApiException;
}
