package com.increff.pos.dao;

import com.increff.pos.db.ClientPojo;
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

    public ClientDao(MongoOperations mongoOperations) {
        super(
                new MongoRepositoryFactory(mongoOperations).getEntityInformation(ClientPojo.class),
                mongoOperations
        );
    }

    public ClientPojo findByEmail(String email) {
        Query query = Query.query(Criteria.where("email").is(email));
        return mongoOperations.findOne(query, ClientPojo.class);
    }

    public Page<ClientPojo> search(String name, String email, Pageable pageable) {
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
            query.addCriteria(new Criteria().andOperator(list));
        }

        return pageableQuery(query, pageable);
    }

    public List<ClientPojo> findByEmails(List<String> emails) {
        if (emails == null || emails.isEmpty()) return List.of();
        Query query = Query.query(Criteria.where("email").in(emails));
        return mongoOperations.find(query, ClientPojo.class);
    }

    // ✅ For product filter: "client" matches name OR email
    public List<String> findEmailsByNameOrEmail(String query, int limit) {
        if (query == null || query.isBlank()) return List.of();

        String safe = Pattern.quote(query.trim());
        Pattern pattern = Pattern.compile(".*" + safe + ".*", Pattern.CASE_INSENSITIVE);

        Query q = new Query();
        q.addCriteria(new Criteria().orOperator(
                Criteria.where("name").regex(pattern),
                Criteria.where("email").regex(pattern)
        ));
        q.limit(Math.max(1, limit));

        return mongoOperations.find(q, ClientPojo.class)
                .stream()
                .map(ClientPojo::getEmail)
                .distinct()
                .toList();
    }
}
