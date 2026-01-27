package com.increff.pos.api;

import com.increff.pos.db.ClientPojo;
import com.increff.pos.db.ClientUpdatePojo;
import com.increff.pos.exception.ApiException;
import org.springframework.data.domain.Page;

public interface ClientApi {
    ClientPojo addClient(ClientPojo clientPojo) throws ApiException;
    ClientPojo getClientById(String id) throws ApiException;
    ClientPojo getClientByEmail(String email) throws ApiException;
    Page<ClientPojo> getAllClients(int page, int size);
    ClientPojo updateClient(ClientUpdatePojo clientUpdatePojo) throws ApiException;
}