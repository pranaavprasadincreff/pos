package com.increff.pos.api;

import com.increff.pos.dao.ClientDao;
import com.increff.pos.db.ClientPojo;
import com.increff.pos.db.ClientUpdatePojo;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.util.EmailNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class ClientApiImpl implements ClientApi {
    private static final Logger logger = LoggerFactory.getLogger(ClientApiImpl.class);

    private final ClientDao dao;
    public ClientApiImpl(ClientDao dao) {
        this.dao = dao;
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public ClientPojo addClient(ClientPojo clientPojo) throws ApiException {
        logger.info("Creating Client with email: {}", clientPojo.getEmail());
        clientPojo.setEmail(EmailNormalizer.normalize(clientPojo.getEmail()));
        checkIfEmailExists(clientPojo);
        ClientPojo saved = dao.save(clientPojo);
        logger.info("Created Client with id: {}", saved.getId());
        return saved;
    }

    @Override
    public ClientPojo getClientById(String id) throws ApiException {
        ClientPojo clientPojo = dao.findById(id).orElse(null);
        if (Objects.isNull(clientPojo)) {
            throw new ApiException("Client not found with id: " + id);
        }
        return clientPojo;
    }

    @Override
    public ClientPojo getClientByEmail(String email) throws ApiException {
        ClientPojo clientPojo = dao.findByEmail(email);
        if (Objects.isNull(clientPojo)) {
            throw new ApiException("Client not found with email: " + email);
        }
        return clientPojo;
    }

    @Override
    public Page<ClientPojo> getAllClients(int page, int size) {
        logger.info("Fetching Clients page {} with size {}", page, size);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return dao.findAll(pageRequest);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public ClientPojo updateClient(ClientUpdatePojo clientUpdatePojo) throws ApiException {
        logger.info("Updating Client with id: {}", clientUpdatePojo.getId());
        clientUpdatePojo.setNewEmail(EmailNormalizer.normalize(clientUpdatePojo.getNewEmail()));
        clientUpdatePojo.setOldEmail(EmailNormalizer.normalize(clientUpdatePojo.getOldEmail()));
        ClientPojo existing = getClientByEmail(clientUpdatePojo.getOldEmail());
        checkIfEmailExistsForUpdate(clientUpdatePojo);
        existing.setName(clientUpdatePojo.getName());
        existing.setEmail(clientUpdatePojo.getNewEmail());
        return dao.save(existing);
    }

    private void checkIfEmailExists(ClientPojo clientPojo) throws ApiException {
        ClientPojo existingClientPojo = dao.findByEmail(clientPojo.getEmail());
        if (existingClientPojo != null) {
            throw new ApiException("Client already exists with email: " + clientPojo.getEmail());
        }
    }

    private void checkIfEmailExistsForUpdate(ClientUpdatePojo clientUpdatePojo) throws ApiException {
        ClientPojo existingClientPojo = dao.findByEmail(clientUpdatePojo.getNewEmail());
        ClientPojo oldClientPojo = dao.findByEmail((clientUpdatePojo.getOldEmail()));
        if (existingClientPojo != null && !existingClientPojo.getId().equals(oldClientPojo.getId())) {
            throw new ApiException("Client already exists with email: " + clientUpdatePojo.getNewEmail());
        }
    }
} 