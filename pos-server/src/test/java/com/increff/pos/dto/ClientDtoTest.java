package com.increff.pos.dto;

import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.ClientForm;
import com.increff.pos.model.form.ClientSearchForm;
import com.increff.pos.model.form.ClientUpdateForm;
import com.increff.pos.model.form.PageForm;
import com.increff.pos.test.AbstractUnitTest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ClientDtoTest extends AbstractUnitTest {
    @Autowired
    private ClientDto clientDto;

    private Validator validator;

    @BeforeEach
    public void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private static void assertNormalizedNameEquals(String expectedRaw, String actualNormalized) {
        assertNotNull(actualNormalized);
        assertEquals(expectedRaw.trim().toLowerCase(), actualNormalized);
    }

    private static void assertHasViolationForField(Set<? extends ConstraintViolation<?>> violations, String fieldName) {
        boolean found = violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(fieldName));
        assertTrue(found, "Expected validation error for field: " + fieldName);
    }

    // ------------------------
    // CREATE (validation is annotation-driven now)
    // ------------------------

    @Test
    public void testCreateFormValidationInvalidEmail() {
        ClientForm form = new ClientForm();
        form.setEmail("invalid-email");
        form.setName("Test Client");

        Set<ConstraintViolation<ClientForm>> violations = validator.validate(form);
        assertFalse(violations.isEmpty());
        assertHasViolationForField(violations, "email");
    }

    @Test
    public void testCreateFormValidationEmptyEmail() {
        ClientForm form = new ClientForm();
        form.setEmail("");
        form.setName("Test Client");

        Set<ConstraintViolation<ClientForm>> violations = validator.validate(form);
        assertFalse(violations.isEmpty());
        assertHasViolationForField(violations, "email");
    }

    @Test
    public void testCreateFormValidationEmptyName() {
        ClientForm form = new ClientForm();
        form.setEmail("test@example.com");
        form.setName("");

        Set<ConstraintViolation<ClientForm>> violations = validator.validate(form);
        assertFalse(violations.isEmpty());
        assertHasViolationForField(violations, "name");
    }

    @Test
    public void testCreateValidClient() throws ApiException {
        ClientForm form = new ClientForm();
        form.setEmail("test@example.com");
        form.setName("Test Client");

        Set<ConstraintViolation<ClientForm>> violations = validator.validate(form);
        assertTrue(violations.isEmpty());

        ClientData createdClient = clientDto.create(form);

        assertNotNull(createdClient);
        assertEquals("test@example.com", createdClient.getEmail());
        assertNormalizedNameEquals("Test Client", createdClient.getName());
    }

    @Test
    public void testCreateDuplicateClientShouldFail() throws ApiException {
        ClientForm form = new ClientForm();
        form.setEmail("duplicate@example.com");
        form.setName("Test Client");

        clientDto.create(form);

        ApiException exception = assertThrows(ApiException.class, () -> clientDto.create(form));
        assertTrue(exception.getMessage().toLowerCase().contains("already exists"));
    }

    @Test
    public void testCreateNormalizesEmailAndName() throws ApiException {
        ClientForm form = new ClientForm();
        form.setEmail("  TeSt@Example.COM  ");
        form.setName("   John   Doe   ");

        ClientData createdClient = clientDto.create(form);

        assertNotNull(createdClient);
        assertEquals("test@example.com", createdClient.getEmail());

        assertFalse(createdClient.getName().startsWith(" "));
        assertFalse(createdClient.getName().endsWith(" "));
        assertTrue(createdClient.getName().contains("john"));
    }

    // ------------------------
    // GET BY EMAIL
    // ------------------------

    @Test
    public void testGetByEmailNotFound() {
        ApiException exception = assertThrows(ApiException.class, () -> clientDto.getByEmail("missing@example.com"));
        assertTrue(exception.getMessage().toLowerCase().contains("not found"));
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
    // GET ALL (via search compatibility)
    // ------------------------

    @Test
    public void testPageFormValidationInvalidPageSize() {
        PageForm pageForm = new PageForm();
        pageForm.setPage(0);
        pageForm.setSize(101);

        Set<ConstraintViolation<PageForm>> violations = validator.validate(pageForm);
        assertFalse(violations.isEmpty());
        assertHasViolationForField(violations, "size");
    }

    @Test
    public void testPageFormValidationInvalidPageNumber() {
        PageForm pageForm = new PageForm();
        pageForm.setPage(-1);
        pageForm.setSize(10);

        Set<ConstraintViolation<PageForm>> violations = validator.validate(pageForm);
        assertFalse(violations.isEmpty());
        assertHasViolationForField(violations, "page");
    }

    @Test
    public void testGetAllUsingSearchValidParams() throws ApiException {
        for (int i = 0; i < 5; i++) {
            ClientForm form = new ClientForm();
            form.setEmail("test" + i + "@example.com");
            form.setName("Test Client " + i);
            clientDto.create(form);
        }

        PageForm pageForm = new PageForm();
        pageForm.setPage(0);
        pageForm.setSize(3);

        Page<ClientData> page = clientDto.getAllUsingSearch(pageForm);

        assertNotNull(page);
        assertTrue(page.getContent().size() <= 3);
        assertTrue(page.getTotalElements() >= 5);
    }

    // ------------------------
    // FILTER
    // ------------------------

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

        Set<ConstraintViolation<ClientSearchForm>> violations = validator.validate(search);
        assertTrue(violations.isEmpty());

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

        Set<ConstraintViolation<ClientSearchForm>> violations = validator.validate(search);
        assertTrue(violations.isEmpty());

        Page<ClientData> page = clientDto.search(search);

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals("second.user@example.com", page.getContent().get(0).getEmail());
    }

    @Test
    public void testSearchFormValidationInvalidPaginationParams() {
        ClientSearchForm search = new ClientSearchForm();
        search.setName("x");
        search.setEmail("y@example.com");
        search.setPage(-1);
        search.setSize(10);

        Set<ConstraintViolation<ClientSearchForm>> violations = validator.validate(search);
        assertFalse(violations.isEmpty());
        assertHasViolationForField(violations, "page");
    }

    // ------------------------
    // UPDATE (validation is annotation-driven now)
    // ------------------------

    @Test
    public void testUpdateFormValidationInvalidOldEmail() {
        ClientUpdateForm form = new ClientUpdateForm();
        form.setOldEmail("not-an-email");
        form.setNewEmail("new@example.com");
        form.setName("New Name");

        Set<ConstraintViolation<ClientUpdateForm>> violations = validator.validate(form);
        assertFalse(violations.isEmpty());
        assertHasViolationForField(violations, "oldEmail");
    }

    @Test
    public void testUpdateFormValidationInvalidNewEmail() {
        ClientUpdateForm form = new ClientUpdateForm();
        form.setOldEmail("old@example.com");
        form.setNewEmail("not-an-email");
        form.setName("New Name");

        Set<ConstraintViolation<ClientUpdateForm>> violations = validator.validate(form);
        assertFalse(violations.isEmpty());
        assertHasViolationForField(violations, "newEmail");
    }

    @Test
    public void testUpdateClientNotFound() {
        ClientUpdateForm form = new ClientUpdateForm();
        form.setOldEmail("missing@example.com");
        form.setNewEmail("new@example.com");
        form.setName("New Name");

        ApiException exception = assertThrows(ApiException.class, () -> clientDto.update(form));
        assertTrue(exception.getMessage().toLowerCase().contains("not found"));
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
        assertNormalizedNameEquals("New Name", updated.getName());
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
        assertNormalizedNameEquals("Updated", updated.getName());
    }
}
