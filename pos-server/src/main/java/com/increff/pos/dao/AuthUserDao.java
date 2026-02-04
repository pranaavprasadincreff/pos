package com.increff.pos.dao;

import com.increff.pos.db.AuthUserPojo;
import com.increff.pos.model.constants.Role;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AuthUserDao extends AbstractDao<AuthUserPojo> {
    public AuthUserDao(MongoOperations mongoOperations) {
        super(
                new MongoRepositoryFactory(mongoOperations)
                        .getEntityInformation(AuthUserPojo.class),
                mongoOperations
        );
    }

    public AuthUserPojo findByEmail(String email) {
        Query q = Query.query(Criteria.where("email").is(email));
        return mongoOperations.findOne(q, AuthUserPojo.class);
    }

    public List<AuthUserPojo> findByRole(Role role) {
        Query q = Query.query(Criteria.where("role").is(role));
        return mongoOperations.find(q, AuthUserPojo.class);
    }
}
