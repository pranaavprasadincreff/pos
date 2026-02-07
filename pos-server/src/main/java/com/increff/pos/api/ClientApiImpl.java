package com.increff.pos.api;

import com.increff.pos.dao.ClientDao;
import com.increff.pos.db.ClientPojo;
import com.increff.pos.model.exception.ApiException;
import jakarta.annotation.Nonnull;
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
    private ClientDao clientDao;

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public ClientPojo add(@Nonnull ClientPojo clientToCreate) throws ApiException {
        validateClientEmailIsUnique(clientToCreate.getEmail());
        return clientDao.save(clientToCreate);
    }

    @Override
    public ClientPojo getClientByEmail(String email) throws ApiException {
        return fetchClientByEmail(email);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public ClientPojo update(@Nonnull ClientPojo clientToUpdate, @Nonnull String oldEmail) throws ApiException {
        ClientPojo existingClient = fetchClientByEmail(oldEmail);

        validateUpdatedEmailDoesNotConflict(existingClient, clientToUpdate.getEmail());
        applyClientUpdate(existingClient, clientToUpdate);

        return clientDao.save(existingClient);
    }


    @Override
    public Page<ClientPojo> search(String name, String email, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return clientDao.search(name, email, pageRequest);
    }

    @Override
    public void validateClientsExist(Set<String> emails) throws ApiException {
        List<ClientPojo> clients = clientDao.findByEmails(new ArrayList<>(emails));
        validateAllClientsExist(emails, clients);
    }

    @Override
    public List<String> findClientEmailsByNameOrEmail(String query, int limit) {
        return clientDao.findEmailsByNameOrEmail(query, limit);
    }

    private ClientPojo fetchClientByEmail(String email) throws ApiException {
        ClientPojo client = clientDao.findByEmail(email);
        if (client == null) {
            throw new ApiException("Client not found with email: " + email);
        }
        return client;
    }

    private void validateClientEmailIsUnique(String email) throws ApiException {
        ClientPojo existingClient = clientDao.findByEmail(email);
        if (existingClient == null) {
            return;
        }
        throw new ApiException("Client already exists with email: " + email);
    }

    private void validateUpdatedEmailDoesNotConflict(ClientPojo existingClient, String newEmail) throws ApiException {
        if (newEmail.equals(existingClient.getEmail())) {
            return;
        }

        ClientPojo clientWithNewEmail = clientDao.findByEmail(newEmail);
        if (clientWithNewEmail == null) {
            return;
        }

        boolean updatedEmailBelongsToSameClient = clientWithNewEmail.getId().equals(existingClient.getId());
        if (updatedEmailBelongsToSameClient) {
            return;
        }
        throw new ApiException("Client already exists with email: " + newEmail);
    }

    private void applyClientUpdate(ClientPojo existingClient, ClientPojo clientToUpdate) {
        existingClient.setName(clientToUpdate.getName());
        existingClient.setEmail(clientToUpdate.getEmail());
    }

    private void validateAllClientsExist(Set<String> emails, List<ClientPojo> clients) throws ApiException {
        boolean allExist = clients.size() == emails.size();
        if (allExist) {
            return;
        }
        throw new ApiException("One or more client emails are invalid");
    }
}
