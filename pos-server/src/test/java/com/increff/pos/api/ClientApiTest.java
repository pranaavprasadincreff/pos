package com.increff.pos.api;

import com.increff.pos.db.ClientPojo;
import com.increff.pos.db.ClientUpdatePojo;
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
    public void testAddDuplicateEmailShouldFail() throws ApiException {
        ClientPojo a = new ClientPojo();
        a.setEmail("dup@example.com");
        a.setName("A");
        clientApi.add(a);

        ClientPojo b = new ClientPojo();
        b.setEmail("dup@example.com");
        b.setName("B");

        assertThrows(ApiException.class, () -> clientApi.add(b));
    }

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
        // ClientApi preserves name casing (no normalization)
        assertEquals("Get Client", fetched.getName());
    }

    @Test
    public void testUpdateClientNotFound() {
        ClientUpdatePojo upd = new ClientUpdatePojo();
        upd.setOldEmail("missing@example.com");
        upd.setNewEmail("new@example.com");
        upd.setName("New Name");

        assertThrows(ApiException.class, () -> clientApi.update(upd));
    }

    @Test
    public void testUpdateWhenNewEmailBelongsToDifferentClientShouldFail() throws ApiException {
        ClientPojo a = new ClientPojo();
        a.setEmail("a@example.com");
        a.setName("A");
        clientApi.add(a);

        ClientPojo b = new ClientPojo();
        b.setEmail("b@example.com");
        b.setName("B");
        clientApi.add(b);

        ClientUpdatePojo upd = new ClientUpdatePojo();
        upd.setOldEmail("a@example.com");
        upd.setNewEmail("b@example.com"); // belongs to other client
        upd.setName("A Updated");

        assertThrows(ApiException.class, () -> clientApi.update(upd));
    }

    @Test
    public void testUpdateSameEmailAllowedAndUpdatesName() throws ApiException {
        ClientPojo a = new ClientPojo();
        a.setEmail("same@example.com");
        a.setName("Old Name");
        ClientPojo saved = clientApi.add(a);

        ClientUpdatePojo upd = new ClientUpdatePojo();
        upd.setOldEmail("same@example.com");
        upd.setNewEmail("same@example.com"); // same client, should be allowed
        upd.setName("New Name");

        ClientPojo updated = clientApi.update(upd);

        assertNotNull(updated);
        assertEquals(saved.getId(), updated.getId());
        assertEquals("same@example.com", updated.getEmail());
        // preserves casing
        assertEquals("New Name", updated.getName());
    }

    @Test
    public void testUpdateChangesEmailAndName() throws ApiException {
        ClientPojo a = new ClientPojo();
        a.setEmail("old@example.com");
        a.setName("Old");
        clientApi.add(a);

        ClientUpdatePojo upd = new ClientUpdatePojo();
        upd.setOldEmail("old@example.com");
        upd.setNewEmail("new@example.com");
        upd.setName("Updated");

        ClientPojo updated = clientApi.update(upd);

        assertNotNull(updated);
        assertEquals("new@example.com", updated.getEmail());
        // preserves casing
        assertEquals("Updated", updated.getName());

        assertThrows(ApiException.class, () -> clientApi.getClientByEmail("old@example.com"));

        ClientPojo fetched = clientApi.getClientByEmail("new@example.com");
        assertEquals(updated.getId(), fetched.getId());
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
