package com.increff.pos.api;

import com.increff.pos.dao.UserDao;
import com.increff.pos.db.UserPojo;
import com.increff.pos.db.UserUpdatePojo;
import com.increff.pos.exception.ApiException;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class UserApiImpl implements UserApi {
    private static final Logger logger = LoggerFactory.getLogger(UserApiImpl.class);

    private final UserDao dao;
    public UserApiImpl(UserDao dao) {
        this.dao = dao;
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public UserPojo addUser(UserPojo userPojo) throws ApiException {
        logger.info("Creating user with email: {}", userPojo.getEmail());
        checkIfEmailExists(userPojo);
        UserPojo saved = dao.save(userPojo);
        logger.info("Created user with id: {}", saved.getId());
        return saved;
    }

    @Override
    public UserPojo getUserById(String id) throws ApiException {
        UserPojo userPojo = dao.findById(id).orElse(null);
        if (Objects.isNull(userPojo)) {
            throw new ApiException("User not found with id: " + id);
        }
        return userPojo;
    }

    @Override
    public UserPojo getUserByEmail(String email) throws ApiException {
        UserPojo userPojo = dao.findByEmail(email);
        if (Objects.isNull(userPojo)) {
            throw new ApiException("User not found with email: " + email);
        }
        return userPojo;
    }

    @Override
    public Page<UserPojo> getAllUsers(int page, int size) {
        logger.info("Fetching users page {} with size {}", page, size);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return dao.findAll(pageRequest);
    }

    @Override
    @Transactional(rollbackFor = ApiException.class)
    public UserPojo updateUser(UserUpdatePojo userUpdatePojo) throws ApiException {
        logger.info("Updating user with id: {}", userUpdatePojo.getId());
        UserPojo existing = getUserByEmail(userUpdatePojo.getOldEmail());
        checkIfEmailExistsForUpdate(userUpdatePojo);
        existing.setName(userUpdatePojo.getName());
        existing.setEmail(userUpdatePojo.getNewEmail());
        return dao.save(existing);
    }

    private void checkIfEmailExists(UserPojo userPojo) throws ApiException {
        UserPojo existingUserPojo = dao.findByEmail(userPojo.getEmail());
        if (existingUserPojo != null) {
            throw new ApiException("User already exists with email: " + userPojo.getEmail());
        }
    }

    private void checkIfEmailExistsForUpdate(UserUpdatePojo userUpdatePojo) throws ApiException {
        UserPojo existingUserPojo = dao.findByEmail(userUpdatePojo.getNewEmail());
        UserPojo oldUserPojo = dao.findByEmail((userUpdatePojo.getOldEmail()));
        if (existingUserPojo != null && !existingUserPojo.getId().equals(oldUserPojo.getId())) {
            throw new ApiException("User already exists with email: " + userUpdatePojo.getNewEmail());
        }
    }
} 