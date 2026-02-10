package com.increff.pos.dao;

import com.increff.pos.db.ClientPojo;
import com.increff.pos.model.form.ClientSearchForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    public Page<ClientPojo> search(ClientSearchForm form) {
        List<Criteria> filters = new ArrayList<>();

        String name = form.getName();
        String email = form.getEmail();

        if (name != null && !name.isBlank()) {
            filters.add(Criteria.where("name").
                    regex(Pattern.compile(".*" + name + ".*", Pattern.CASE_INSENSITIVE)));
        }
        if (email != null && !email.isBlank()) {
            filters.add(Criteria.where("email").
                    regex(Pattern.compile(".*" + email + ".*", Pattern.CASE_INSENSITIVE)));
        }

        Query query = new Query();
        if (!filters.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(filters));
        }

        Pageable pageable = PageRequest.of(
                form.getPage(),
                form.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return pageableQuery(query, pageable);
    }

    public List<ClientPojo> findByEmails(List<String> emails) {
        if (emails == null || emails.isEmpty()) return List.of();
        Query query = Query.query(Criteria.where("email").in(emails));
        return mongoOperations.find(query, ClientPojo.class);
    }

    public List<String> findEmailsByNameOrEmail(String clientQuery, int limit) {
        if (clientQuery == null || clientQuery.isBlank()) return List.of();

        Pattern pattern = Pattern.compile(".*" + clientQuery + ".*", Pattern.CASE_INSENSITIVE);;

        Query query = new Query();
        query.addCriteria(new Criteria().orOperator(
                Criteria.where("name").regex(pattern),
                Criteria.where("email").regex(pattern)
        ));
        query.limit(Math.max(1, limit));

        return mongoOperations.find(query, ClientPojo.class)
                .stream()
                .map(ClientPojo::getEmail)
                .distinct()
                .toList();
    }
}
