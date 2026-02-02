package com.increff.pos.api;

import com.increff.pos.dao.ClientDao;
import com.increff.pos.db.ClientPojo;
import com.increff.pos.db.ClientUpdatePojo;
import com.increff.pos.model.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ClientApiImpl implements ClientApi {
    @Autowired
    private ClientDao dao;

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public ClientPojo add(ClientPojo pojo) throws ApiException {
        ensureEmailUnique(pojo.getEmail());
        return dao.save(pojo);
    }

    @Override
    public ClientPojo getClientByEmail(String email) throws ApiException {
        // TODO change name getByEmailOrThrow -> getCheckByEmail
        return getByEmailOrThrow(email);
    }

    @Override
    public Page<ClientPojo> getAll(int page, int size) {
        PageRequest req = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return dao.findAll(req);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public ClientPojo update(ClientUpdatePojo update) throws ApiException {
        ClientPojo existing = getByEmailOrThrow(update.getOldEmail());
        ensureEmailUniqueForUpdate(update);
        // TODO change applyUpdate name
        applyUpdate(existing, update);
        return dao.save(existing);
    }

    @Override
    public Page<ClientPojo> filter(String name, String email, int page, int size) {
        PageRequest req = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return dao.filter(name, email, req);
    }

    @Override
    public void validateClientsExist(Set<String> emails) throws ApiException {
        List<ClientPojo> clients = dao.findByEmails(new ArrayList<>(emails));

        if (clients.size() != emails.size()) {
            throw new ApiException("One or more client emails are invalid");
        }
    }

    private ClientPojo getByEmailOrThrow(String email) throws ApiException {
        ClientPojo pojo = dao.findByEmail(email);
        if (pojo != null) {
            return pojo;
        }
        throw new ApiException("Client not found with email: " + email);
    }

    private void ensureEmailUnique(String email) throws ApiException {
        ClientPojo existing = dao.findByEmail(email);
        if (existing == null) {
            return;
        }
        throw new ApiException("Client already exists with email: " + email);
    }

    private void ensureEmailUniqueForUpdate(ClientUpdatePojo update) throws ApiException {
        ClientPojo existing = dao.findByEmail(update.getNewEmail());
        ClientPojo oldPojo = dao.findByEmail(update.getOldEmail());

        if (existing == null) {
            return;
        }

        String existingId = existing.getId();
        String oldId = oldPojo.getId();
        boolean different = !existingId.equals(oldId);

        if (different) {
            throw new ApiException("Client already exists with email: " + update.getNewEmail());
        }
    }

    private void applyUpdate(ClientPojo pojo, ClientUpdatePojo update) {
        pojo.setName(update.getName());
        pojo.setEmail(update.getNewEmail());
    }
}
