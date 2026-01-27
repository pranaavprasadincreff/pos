package com.increff.pos.dto;

import com.increff.pos.test.AbstractUnitTest;
import com.increff.pos.exception.ApiException;
import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.form.PageForm;
import com.increff.pos.model.form.ClientForm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import static org.junit.jupiter.api.Assertions.*;

public class ClientDtoTest extends AbstractUnitTest {
    
    @Autowired
    private ClientDto clientDto;
    
    @Test
    public void testCreateClientWithInvalidEmail() {
        ClientForm form = new ClientForm();
        form.setEmail("invalid-email");
        form.setName("Test Client");
        assertThrows(ApiException.class, () -> clientDto.createClient(form));
    }

    @Test
    public void testCreateClientWithEmptyEmail() {
        ClientForm form = new ClientForm();
        form.setEmail("");
        form.setName("Test Client");
        assertThrows(ApiException.class, () -> clientDto.createClient(form));
    }

    @Test
    public void testCreateClientWithEmptyName() {
        ClientForm form = new ClientForm();
        form.setEmail("test@example.com");
        form.setName("");
        assertThrows(ApiException.class, () -> clientDto.createClient(form));
    }

    @Test
    public void testCreateValidClient() throws ApiException {
        ClientForm form = new ClientForm();
        form.setEmail("test@example.com");
        form.setName("Test Client");
        
        ClientData clientData = clientDto.createClient(form);
        
        assertNotNull(clientData);
        assertEquals(form.getEmail(), clientData.getEmail());
        assertEquals(form.getName(), clientData.getName());
        assertNotNull(clientData.getId());
    }

    @Test
    public void testCreateDuplicateClient() throws ApiException {
        ClientForm form = new ClientForm();
        form.setEmail("duplicate@example.com");
        form.setName("Test Client");
        
        clientDto.createClient(form);
        assertThrows(ApiException.class, () -> clientDto.createClient(form));
    }

    @Test
    public void testGetByIdNonExistent() {
        String nonExistentId = "non-existent-id";
        assertThrows(ApiException.class, () -> clientDto.getById(nonExistentId));
    }

    @Test
    public void testGetByIdExisting() throws ApiException {
        // First create a client
        ClientForm form = new ClientForm();
        form.setEmail("get-by-id@example.com");
        form.setName("Test Client");
        ClientData created = clientDto.createClient(form);
        
        // Then retrieve it
        ClientData retrieved = clientDto.getById(created.getId());
        
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals(created.getEmail(), retrieved.getEmail());
        assertEquals(created.getName(), retrieved.getName());
    }

    @Test
    public void testPaginationInvalidPageSize() {
        PageForm form = new PageForm();
        form.setPage(0);
        form.setSize(101); // Max size is 100
        
        assertThrows(ApiException.class, () -> clientDto.getAllClients(form));
    }

    @Test
    public void testPaginationInvalidPageNumber() {
        PageForm form = new PageForm();
        form.setPage(-1);
        form.setSize(10);
        
        assertThrows(ApiException.class, () -> clientDto.getAllClients(form));
    }

    @Test
    public void testPaginationValidParams() throws ApiException {
        // Create some test clients
        for (int i = 0; i < 5; i++) {
            ClientForm form = new ClientForm();
            form.setEmail("test" + i + "@example.com");
            form.setName("Test Client " + i);
            clientDto.createClient(form);
        }
        
        PageForm pageForm = new PageForm();
        pageForm.setPage(0);
        pageForm.setSize(3);
        
        Page<ClientData> page = clientDto.getAllClients(pageForm);
        
        assertNotNull(page);
        assertTrue(page.getContent().size() <= 3);
        assertTrue(page.getTotalElements() >= 5);
    }
} 