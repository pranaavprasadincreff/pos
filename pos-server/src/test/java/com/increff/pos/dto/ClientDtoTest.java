package com.increff.pos.dto;

import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.ClientFilterForm;
import com.increff.pos.model.form.ClientForm;
import com.increff.pos.model.form.ClientUpdateForm;
import com.increff.pos.model.form.PageForm;
import com.increff.pos.test.AbstractUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import static org.junit.jupiter.api.Assertions.*;

public class ClientDtoTest extends AbstractUnitTest {

    @Autowired
    private ClientDto clientDto;

    // NormalizeUtil lowercases names in your project, so assert accordingly.
    private static void assertNormalizedNameEquals(String expectedRaw, String actualNormalized) {
        assertNotNull(actualNormalized);
        assertEquals(expectedRaw.trim().toLowerCase(), actualNormalized);
    }

    // ------------------------
    // CREATE
    // ------------------------

    @Test
    public void testCreateClientWithInvalidEmail() {
        ClientForm form = new ClientForm();
        form.setEmail("invalid-email");
        form.setName("Test Client");
        assertThrows(ApiException.class, () -> clientDto.create(form));
    }

    @Test
    public void testCreateClientWithEmptyEmail() {
        ClientForm form = new ClientForm();
        form.setEmail("");
        form.setName("Test Client");
        assertThrows(ApiException.class, () -> clientDto.create(form));
    }

    @Test
    public void testCreateClientWithEmptyName() {
        ClientForm form = new ClientForm();
        form.setEmail("test@example.com");
        form.setName("");
        assertThrows(ApiException.class, () -> clientDto.create(form));
    }

    @Test
    public void testCreateValidClient() throws ApiException {
        ClientForm form = new ClientForm();
        form.setEmail("test@example.com");
        form.setName("Test Client");

        ClientData created = clientDto.create(form);

        assertNotNull(created);
        assertEquals("test@example.com", created.getEmail());
        assertNormalizedNameEquals("Test Client", created.getName());
    }

    @Test
    public void testCreateDuplicateClient() throws ApiException {
        ClientForm form = new ClientForm();
        form.setEmail("duplicate@example.com");
        form.setName("Test Client");

        clientDto.create(form);
        assertThrows(ApiException.class, () -> clientDto.create(form));
    }

    @Test
    public void testCreateNormalizesEmailAndName() throws ApiException {
        ClientForm form = new ClientForm();
        form.setEmail("  TeSt@Example.COM  ");
        form.setName("   John   Doe   ");

        ClientData created = clientDto.create(form);

        assertNotNull(created);
        assertEquals("test@example.com", created.getEmail());

        // Don't assume whitespace collapsing rules beyond trim+lowercase unless you want to mirror util exactly
        assertFalse(created.getName().startsWith(" "));
        assertFalse(created.getName().endsWith(" "));
        assertTrue(created.getName().contains("john"));
    }

    // ------------------------
    // GET BY EMAIL
    // ------------------------

    @Test
    public void testGetByEmailInvalidEmail() {
        assertThrows(ApiException.class, () -> clientDto.getByEmail("not-an-email"));
    }

    @Test
    public void testGetByEmailNotFound() {
        ApiException ex = assertThrows(ApiException.class, () -> clientDto.getByEmail("missing@example.com"));
        assertTrue(ex.getMessage().toLowerCase().contains("not found"));
    }

    @Test
    public void testGetByEmailNormalizesInputEmail() throws ApiException {
        ClientForm form = new ClientForm();
        form.setEmail("test@example.com");
        form.setName("Test Client");
        clientDto.create(form);

        ClientData fetched = clientDto.getByEmail("  TEST@EXAMPLE.COM  ");

        assertNotNull(fetched);
        assertEquals("test@example.com", fetched.getEmail());
        assertNormalizedNameEquals("Test Client", fetched.getName());
    }

    // ------------------------
    // PAGINATION
    // ------------------------

    @Test
    public void testPaginationInvalidPageSize() {
        PageForm form = new PageForm();
        form.setPage(0);
        form.setSize(101); // Max size 100 per your ValidationUtil

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
        for (int i = 0; i < 5; i++) {
            ClientForm form = new ClientForm();
            form.setEmail("test" + i + "@example.com");
            form.setName("Test Client " + i);
            clientDto.create(form);
        }

        PageForm pageForm = new PageForm();
        pageForm.setPage(0);
        pageForm.setSize(3);

        Page<ClientData> page = clientDto.getAllClients(pageForm);

        assertNotNull(page);
        assertTrue(page.getContent().size() <= 3);
        assertTrue(page.getTotalElements() >= 5);
    }

    // ------------------------
    // FILTER
    // ------------------------

    @Test
    public void testFilterValidByNameCaseInsensitive() throws ApiException {
        ClientForm a = new ClientForm();
        a.setEmail("alice@example.com");
        a.setName("Alice Johnson");
        clientDto.create(a);

        ClientForm b = new ClientForm();
        b.setEmail("bob@example.com");
        b.setName("Bob Smith");
        clientDto.create(b);

        ClientFilterForm filter = new ClientFilterForm();
        filter.setName("aLiCe");
        filter.setEmail(null);
        filter.setPage(0);
        filter.setSize(10);

        Page<ClientData> page = clientDto.filter(filter);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals("alice@example.com", page.getContent().get(0).getEmail());
    }

    @Test
    public void testFilterValidByEmailCaseInsensitivePartial() throws ApiException {
        ClientForm a = new ClientForm();
        a.setEmail("first.user@example.com");
        a.setName("First User");
        clientDto.create(a);

        ClientForm b = new ClientForm();
        b.setEmail("second.user@example.com");
        b.setName("Second User");
        clientDto.create(b);

        ClientFilterForm filter = new ClientFilterForm();
        filter.setName(null);
        filter.setEmail("SECOND.USER");
        filter.setPage(0);
        filter.setSize(10);

        Page<ClientData> page = clientDto.filter(filter);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals("second.user@example.com", page.getContent().get(0).getEmail());
    }

    @Test
    public void testFilterInvalidPaginationParams() {
        ClientFilterForm filter = new ClientFilterForm();
        filter.setName("x");
        filter.setEmail("y@example.com");
        filter.setPage(-1);
        filter.setSize(10);

        assertThrows(ApiException.class, () -> clientDto.filter(filter));
    }

    // ------------------------
    // UPDATE
    // ------------------------

    @Test
    public void testUpdateInvalidOldEmail() {
        ClientUpdateForm form = new ClientUpdateForm();
        form.setOldEmail("not-an-email");
        form.setNewEmail("new@example.com");
        form.setName("New Name");

        assertThrows(ApiException.class, () -> clientDto.update(form));
    }

    @Test
    public void testUpdateInvalidNewEmail() {
        ClientUpdateForm form = new ClientUpdateForm();
        form.setOldEmail("old@example.com");
        form.setNewEmail("not-an-email");
        form.setName("New Name");

        assertThrows(ApiException.class, () -> clientDto.update(form));
    }

    @Test
    public void testUpdateClientNotFound() {
        ClientUpdateForm form = new ClientUpdateForm();
        form.setOldEmail("missing@example.com");
        form.setNewEmail("new@example.com");
        form.setName("New Name");

        assertThrows(ApiException.class, () -> clientDto.update(form));
    }

    @Test
    public void testUpdateDuplicateNewEmailShouldFail() throws ApiException {
        ClientForm a = new ClientForm();
        a.setEmail("a@example.com");
        a.setName("A");
        clientDto.create(a);

        ClientForm b = new ClientForm();
        b.setEmail("b@example.com");
        b.setName("B");
        clientDto.create(b);

        ClientUpdateForm upd = new ClientUpdateForm();
        upd.setOldEmail("a@example.com");
        upd.setNewEmail("b@example.com"); // already exists for a different client
        upd.setName("A Updated");

        assertThrows(ApiException.class, () -> clientDto.update(upd));
    }

    @Test
    public void testUpdateSameEmailAllowedAndUpdatesName() throws ApiException {
        ClientForm a = new ClientForm();
        a.setEmail("same@example.com");
        a.setName("Old Name");
        clientDto.create(a);

        ClientUpdateForm upd = new ClientUpdateForm();
        upd.setOldEmail("same@example.com");
        upd.setNewEmail("same@example.com"); // same email should be allowed
        upd.setName("New Name");

        ClientData updated = clientDto.update(upd);

        assertNotNull(updated);
        assertEquals("same@example.com", updated.getEmail());
        assertNormalizedNameEquals("New Name", updated.getName());
    }

    @Test
    public void testUpdateNormalizesNewEmail() throws ApiException {
        ClientForm a = new ClientForm();
        a.setEmail("old@example.com");
        a.setName("Old");
        clientDto.create(a);

        ClientUpdateForm upd = new ClientUpdateForm();
        upd.setOldEmail("old@example.com");
        upd.setNewEmail("  NEW@EXAMPLE.COM  ");
        upd.setName("Updated");

        ClientData updated = clientDto.update(upd);

        assertNotNull(updated);
        assertEquals("new@example.com", updated.getEmail());
        assertNormalizedNameEquals("Updated", updated.getName());
    }
}
