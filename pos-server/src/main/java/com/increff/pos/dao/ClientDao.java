package com.increff.pos.dao;

import com.increff.pos.db.ClientPojo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Repository
public class ClientDao extends AbstractDao<ClientPojo> {

    @Autowired
    public ClientDao(MongoOperations mongoOperations) {
        super(
                new MongoRepositoryFactory(mongoOperations)
                        .getEntityInformation(ClientPojo.class),
                mongoOperations
        );
    }

    public ClientPojo findByEmail(String email) {
        Query query = Query.query(Criteria.where("email").is(email));
        return mongoOperations.findOne(query, ClientPojo.class);
    }

    public Page<ClientPojo> filter(String name, String email, Pageable pageable) {
        List<Criteria> list = new ArrayList<>();

        if (name != null && !name.isBlank()) {
            Pattern pattern = Pattern.compile(Pattern.quote(name), Pattern.CASE_INSENSITIVE);
            list.add(Criteria.where("name").regex(pattern));
        }
        if (email != null && !email.isBlank()) {
            Pattern pattern = Pattern.compile(Pattern.quote(email), Pattern.CASE_INSENSITIVE);
            list.add(Criteria.where("email").regex(pattern));
        }
        Query query = new Query();
        if (!list.isEmpty()) {
            Criteria combined = new Criteria().andOperator(list);
            query.addCriteria(combined);
        }

        return pageableQuery(query, pageable);
    }

    public List<ClientPojo> findByEmails(List<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return List.of();
        }

        Query query = Query.query(Criteria.where("email").in(emails));
        return mongoOperations.find(query, ClientPojo.class);
    }

}
