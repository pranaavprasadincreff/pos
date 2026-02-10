package com.increff.pos.api;

import com.increff.pos.db.ClientPojo;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.test.AbstractUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ClientApiTest extends AbstractUnitTest {

    @Autowired
    private ClientApi clientApi;

    @Test
    public void testGetClientByEmailNotFound() {
        ApiException ex = assertThrows(ApiException.class,
                () -> clientApi.getClientByEmail("missing@example.com"));
        assertTrue(ex.getMessage().toLowerCase().contains("not found"));
    }

    @Test
    public void testGetClientByEmailSuccess() throws ApiException {
        ClientPojo pojo = new ClientPojo();
        pojo.setEmail("get@example.com");
        pojo.setName("Get Client");
        clientApi.add(pojo);

        ClientPojo fetched = clientApi.getClientByEmail("get@example.com");

        assertNotNull(fetched);
        assertEquals("get@example.com", fetched.getEmail());
        assertEquals("Get Client", fetched.getName());
    }

    @Test
    public void testUpdatePersistsChanges_whenPreparedPojoIsPassed() throws ApiException {
        // create
        ClientPojo a = new ClientPojo();
        a.setEmail("old@example.com");
        a.setName("Old Name");
        clientApi.add(a);

        // fetch existing and prepare updates (this orchestration is DTO responsibility now,
        // but for API test we prepare the final pojo ourselves)
        ClientPojo existing = clientApi.getClientByEmail("old@example.com");
        existing.setEmail("new@example.com");
        existing.setName("New Name");

        ClientPojo updated = clientApi.update(existing);

        assertNotNull(updated);
        assertEquals("new@example.com", updated.getEmail());
        assertEquals("New Name", updated.getName());

        // verify persisted
        assertThrows(ApiException.class, () -> clientApi.getClientByEmail("old@example.com"));
        ClientPojo fetched = clientApi.getClientByEmail("new@example.com");
        assertEquals(updated.getId(), fetched.getId());
        assertEquals("New Name", fetched.getName());
    }

    @Test
    public void testValidateClientsExistAllValid() throws ApiException {
        ClientPojo a = new ClientPojo();
        a.setEmail("a@example.com");
        a.setName("A");
        clientApi.add(a);

        ClientPojo b = new ClientPojo();
        b.setEmail("b@example.com");
        b.setName("B");
        clientApi.add(b);

        assertDoesNotThrow(() -> clientApi.validateClientsExist(Set.of(
                "a@example.com", "b@example.com"
        )));
    }

    @Test
    public void testValidateClientsExistSomeInvalid() throws ApiException {
        ClientPojo a = new ClientPojo();
        a.setEmail("a@example.com");
        a.setName("A");
        clientApi.add(a);

        ApiException ex = assertThrows(ApiException.class,
                () -> clientApi.validateClientsExist(Set.of(
                        "a@example.com", "missing@example.com"
                )));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid"));
    }
}
