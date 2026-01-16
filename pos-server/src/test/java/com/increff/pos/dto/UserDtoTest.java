package com.increff.pos.dto;

import com.increff.pos.test.AbstractUnitTest;
import com.increff.pos.exception.ApiException;
import com.increff.pos.model.data.UserData;
import com.increff.pos.model.form.PageForm;
import com.increff.pos.model.form.UserForm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import static org.junit.jupiter.api.Assertions.*;

public class UserDtoTest extends AbstractUnitTest {
    
    @Autowired
    private UserDto userDto;
    
    @Test
    public void testCreateUserWithInvalidEmail() {
        UserForm form = new UserForm();
        form.setEmail("invalid-email");
        form.setName("Test User");
        assertThrows(ApiException.class, () -> userDto.createUser(form));
    }

    @Test
    public void testCreateUserWithEmptyEmail() {
        UserForm form = new UserForm();
        form.setEmail("");
        form.setName("Test User");
        assertThrows(ApiException.class, () -> userDto.createUser(form));
    }

    @Test
    public void testCreateUserWithEmptyName() {
        UserForm form = new UserForm();
        form.setEmail("test@example.com");
        form.setName("");
        assertThrows(ApiException.class, () -> userDto.createUser(form));
    }

    @Test
    public void testCreateValidUser() throws ApiException {
        UserForm form = new UserForm();
        form.setEmail("test@example.com");
        form.setName("Test User");
        
        UserData userData = userDto.createUser(form);
        
        assertNotNull(userData);
        assertEquals(form.getEmail(), userData.getEmail());
        assertEquals(form.getName(), userData.getName());
        assertNotNull(userData.getId());
    }

    @Test
    public void testCreateDuplicateUser() throws ApiException {
        UserForm form = new UserForm();
        form.setEmail("duplicate@example.com");
        form.setName("Test User");
        
        userDto.createUser(form);
        assertThrows(ApiException.class, () -> userDto.createUser(form));
    }

    @Test
    public void testGetByIdNonExistent() {
        String nonExistentId = "non-existent-id";
        assertThrows(ApiException.class, () -> userDto.getById(nonExistentId));
    }

    @Test
    public void testGetByIdExisting() throws ApiException {
        // First create a user
        UserForm form = new UserForm();
        form.setEmail("get-by-id@example.com");
        form.setName("Test User");
        UserData created = userDto.createUser(form);
        
        // Then retrieve it
        UserData retrieved = userDto.getById(created.getId());
        
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
        
        assertThrows(ApiException.class, () -> userDto.getAllUsers(form));
    }

    @Test
    public void testPaginationInvalidPageNumber() {
        PageForm form = new PageForm();
        form.setPage(-1);
        form.setSize(10);
        
        assertThrows(ApiException.class, () -> userDto.getAllUsers(form));
    }

    @Test
    public void testPaginationValidParams() throws ApiException {
        // Create some test users
        for (int i = 0; i < 5; i++) {
            UserForm form = new UserForm();
            form.setEmail("test" + i + "@example.com");
            form.setName("Test User " + i);
            userDto.createUser(form);
        }
        
        PageForm pageForm = new PageForm();
        pageForm.setPage(0);
        pageForm.setSize(3);
        
        Page<UserData> page = userDto.getAllUsers(pageForm);
        
        assertNotNull(page);
        assertTrue(page.getContent().size() <= 3);
        assertTrue(page.getTotalElements() >= 5);
    }
} 