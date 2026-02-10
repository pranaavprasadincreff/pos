package com.increff.pos.api;

import com.increff.pos.dao.ClientDao;
import com.increff.pos.db.ClientPojo;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.ClientSearchForm;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ClientApiImpl implements ClientApi {

    @Autowired
    private ClientDao clientDao;

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public ClientPojo add(@Nonnull ClientPojo clientToCreate) {
        return clientDao.save(clientToCreate);
    }

    @Override
    public ClientPojo getClientByEmail(String email) throws ApiException {
        ClientPojo client = clientDao.findByEmail(email);
        if (client == null) {
            throw new ApiException("CLIENT_NOT_FOUND", "Client not found with email: " + email);
        }
        return client;
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public ClientPojo update(@Nonnull ClientPojo clientToUpdate) {
        return clientDao.save(clientToUpdate);
    }

    @Override
    public Page<ClientPojo> search(ClientSearchForm form) {
        return clientDao.search(form);
    }

    @Override
    public void validateClientsExist(Set<String> emails) throws ApiException {
        List<ClientPojo> clients = clientDao.findByEmails(new ArrayList<>(emails));
        if (clients.size() != emails.size()) {
            throw new ApiException("One or more client emails are invalid");
        }
    }

    @Override
    public List<String> findClientEmailsByNameOrEmail(String query, int limit) {
        return clientDao.findEmailsByNameOrEmail(query, limit);
    }
}
