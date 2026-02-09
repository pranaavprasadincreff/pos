package com.increff.pos.dto;

import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.ClientForm;
import com.increff.pos.model.form.ClientSearchForm;
import com.increff.pos.model.form.ClientUpdateForm;
import com.increff.pos.test.AbstractUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import static org.junit.jupiter.api.Assertions.*;

public class ClientDtoTest extends AbstractUnitTest {

    @Autowired
    private ClientDto clientDto;

    private static void assertNormalizedEquals(String expectedRaw, String actualNormalized) {
        assertNotNull(actualNormalized);
        assertEquals(expectedRaw.trim().toLowerCase(), actualNormalized);
    }

    private static void assertApiExceptionContains(ApiException e, String containsLower) {
        assertNotNull(e.getMessage());
        assertTrue(e.getMessage().toLowerCase().contains(containsLower),
                "Expected error to contain: " + containsLower + " but got: " + e.getMessage());
    }

    // ------------------------
    // CREATE
    // ------------------------

    @Test
    public void testCreateInvalidEmailShouldFail() {
        ClientForm form = new ClientForm();
        form.setEmail("invalid-email");
        form.setName("Test Client");

        ApiException e = assertThrows(ApiException.class, () -> clientDto.create(form));
        // message can differ; ensure the key field is mentioned
        assertTrue(e.getMessage().toLowerCase().contains("email"));
    }

    @Test
    public void testCreateEmptyEmailShouldFail() {
        ClientForm form = new ClientForm();
        form.setEmail("");
        form.setName("Test Client");

        ApiException e = assertThrows(ApiException.class, () -> clientDto.create(form));
        assertTrue(e.getMessage().toLowerCase().contains("email"));
    }

    @Test
    public void testCreateEmptyNameShouldFail() {
        ClientForm form = new ClientForm();
        form.setEmail("test@example.com");
        form.setName("");

        ApiException e = assertThrows(ApiException.class, () -> clientDto.create(form));
        assertTrue(e.getMessage().toLowerCase().contains("name"));
    }

    @Test
    public void testCreateValidClient() throws ApiException {
        ClientForm form = new ClientForm();
        form.setEmail("test@example.com");
        form.setName("Test Client");

        ClientData createdClient = clientDto.create(form);

        assertNotNull(createdClient);
        assertEquals("test@example.com", createdClient.getEmail());
        assertNormalizedEquals("Test Client", createdClient.getName());
    }

    @Test
    public void testCreateDuplicateClientShouldFail() throws ApiException {
        ClientForm form = new ClientForm();
        form.setEmail("duplicate@example.com");
        form.setName("Test Client");

        clientDto.create(form);

        ApiException exception = assertThrows(ApiException.class, () -> clientDto.create(form));
        assertApiExceptionContains(exception, "already");
    }

    @Test
    public void testCreateNormalizesEmailAndName() throws ApiException {
        ClientForm form = new ClientForm();
        form.setEmail("  TeSt@Example.COM  ");
        form.setName("   John   Doe   ");

        ClientData createdClient = clientDto.create(form);

        assertNotNull(createdClient);
        assertEquals("test@example.com", createdClient.getEmail());
        assertEquals("john   doe", createdClient.getName()); // trims + lowercases; preserves internal spaces
    }

    // ------------------------
    // GET BY EMAIL
    // ------------------------

    @Test
    public void testGetByEmailNotFound() {
        ApiException exception = assertThrows(ApiException.class,
                () -> clientDto.getByEmail("missing@example.com"));
        assertApiExceptionContains(exception, "not found");
    }

    @Test
    public void testGetByEmailInvalidEmailShouldFailFast() {
        ApiException exception = assertThrows(ApiException.class,
                () -> clientDto.getByEmail("not-an-email"));
        assertTrue(exception.getMessage().toLowerCase().contains("email"));
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
        assertNormalizedEquals("Test Client", fetched.getName());
    }

    // ------------------------
    // SEARCH (also substitutes "get all paginated")
    // ------------------------

    @Test
    public void testSearchEmptyFiltersReturnsAllPaginated() throws ApiException {
        for (int i = 0; i < 5; i++) {
            ClientForm form = new ClientForm();
            form.setEmail("test" + i + "@example.com");
            form.setName("Test Client " + i);
            clientDto.create(form);
        }

        ClientSearchForm search = new ClientSearchForm();
        search.setName(null);
        search.setEmail(null);
        search.setPage(0);
        search.setSize(3);

        Page<ClientData> page = clientDto.search(search);

        assertNotNull(page);
        assertTrue(page.getContent().size() <= 3);
        assertTrue(page.getTotalElements() >= 5);
    }

    @Test
    public void testSearchValidByNameCaseInsensitive() throws ApiException {
        ClientForm alice = new ClientForm();
        alice.setEmail("alice@example.com");
        alice.setName("Alice Johnson");
        clientDto.create(alice);

        ClientForm bob = new ClientForm();
        bob.setEmail("bob@example.com");
        bob.setName("Bob Smith");
        clientDto.create(bob);

        ClientSearchForm search = new ClientSearchForm();
        search.setName("aLiCe");
        search.setEmail(null);
        search.setPage(0);
        search.setSize(10);

        Page<ClientData> page = clientDto.search(search);

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals("alice@example.com", page.getContent().get(0).getEmail());
    }

    @Test
    public void testSearchValidByEmailCaseInsensitivePartial() throws ApiException {
        ClientForm first = new ClientForm();
        first.setEmail("first.user@example.com");
        first.setName("First User");
        clientDto.create(first);

        ClientForm second = new ClientForm();
        second.setEmail("second.user@example.com");
        second.setName("Second User");
        clientDto.create(second);

        ClientSearchForm search = new ClientSearchForm();
        search.setName(null);
        search.setEmail("SECOND.USER");
        search.setPage(0);
        search.setSize(10);

        Page<ClientData> page = clientDto.search(search);

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals("second.user@example.com", page.getContent().get(0).getEmail());
    }

    @Test
    public void testSearchInvalidPaginationParamsShouldFail() {
        ClientSearchForm search = new ClientSearchForm();
        search.setName("x");
        search.setEmail("y@example.com");
        search.setPage(-1);
        search.setSize(10);

        ApiException e = assertThrows(ApiException.class, () -> clientDto.search(search));
        assertTrue(e.getMessage().toLowerCase().contains("page"));
    }

    @Test
    public void testSearchTooLongNameShouldFail() {
        ClientSearchForm search = new ClientSearchForm();
        search.setName("x".repeat(31)); // NAME_MAX + 1
        search.setEmail(null);
        search.setPage(0);
        search.setSize(10);

        ApiException e = assertThrows(ApiException.class, () -> clientDto.search(search));
        assertTrue(e.getMessage().toLowerCase().contains("name"));
    }

    @Test
    public void testSearchBlankNameBecomesNullAndReturnsAll() throws ApiException {
        for (int i = 0; i < 3; i++) {
            ClientForm form = new ClientForm();
            form.setEmail("blank" + i + "@example.com");
            form.setName("Blank " + i);
            clientDto.create(form);
        }

        ClientSearchForm search = new ClientSearchForm();
        search.setName("   "); // should normalize to null
        search.setEmail("   "); // should normalize to null
        search.setPage(0);
        search.setSize(10);

        Page<ClientData> page = clientDto.search(search);

        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 3);
    }

    // ------------------------
    // UPDATE
    // ------------------------

    @Test
    public void testUpdateFormInvalidOldEmailShouldFail() {
        ClientUpdateForm form = new ClientUpdateForm();
        form.setOldEmail("not-an-email");
        form.setNewEmail("new@example.com");
        form.setName("New Name");

        ApiException e = assertThrows(ApiException.class, () -> clientDto.update(form));
        assertTrue(e.getMessage().toLowerCase().contains("oldemail") || e.getMessage().toLowerCase().contains("email"));
    }

    @Test
    public void testUpdateFormInvalidNewEmailShouldFail() {
        ClientUpdateForm form = new ClientUpdateForm();
        form.setOldEmail("old@example.com");
        form.setNewEmail("not-an-email");
        form.setName("New Name");

        ApiException e = assertThrows(ApiException.class, () -> clientDto.update(form));
        assertTrue(e.getMessage().toLowerCase().contains("newemail") || e.getMessage().toLowerCase().contains("email"));
    }

    @Test
    public void testUpdateClientNotFound() {
        ClientUpdateForm form = new ClientUpdateForm();
        form.setOldEmail("missing@example.com");
        form.setNewEmail("new@example.com");
        form.setName("New Name");

        ApiException exception = assertThrows(ApiException.class, () -> clientDto.update(form));
        assertApiExceptionContains(exception, "not found");
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

        ClientUpdateForm updateForm = new ClientUpdateForm();
        updateForm.setOldEmail("a@example.com");
        updateForm.setNewEmail("b@example.com");
        updateForm.setName("A Updated");

        assertThrows(ApiException.class, () -> clientDto.update(updateForm));
    }

    @Test
    public void testUpdateSameEmailAllowedAndUpdatesName() throws ApiException {
        ClientForm a = new ClientForm();
        a.setEmail("same@example.com");
        a.setName("Old Name");
        clientDto.create(a);

        ClientUpdateForm updateForm = new ClientUpdateForm();
        updateForm.setOldEmail("same@example.com");
        updateForm.setNewEmail("same@example.com");
        updateForm.setName("New Name");

        ClientData updated = clientDto.update(updateForm);

        assertNotNull(updated);
        assertEquals("same@example.com", updated.getEmail());
        assertNormalizedEquals("New Name", updated.getName());
    }

    @Test
    public void testUpdateNormalizesNewEmail() throws ApiException {
        ClientForm a = new ClientForm();
        a.setEmail("old@example.com");
        a.setName("Old");
        clientDto.create(a);

        ClientUpdateForm updateForm = new ClientUpdateForm();
        updateForm.setOldEmail("old@example.com");
        updateForm.setNewEmail("  NEW@EXAMPLE.COM  ");
        updateForm.setName("Updated");

        ClientData updated = clientDto.update(updateForm);

        assertNotNull(updated);
        assertEquals("new@example.com", updated.getEmail());
        assertNormalizedEquals("Updated", updated.getName());
    }
}
