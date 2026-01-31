package com.increff.pos.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.support.SimpleMongoRepository;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;

import java.util.List;

public abstract class AbstractDao<T> extends SimpleMongoRepository<T, String> {

    protected final MongoOperations mongoOperations;
    private final Class<T> entityClass;

    public AbstractDao(MongoEntityInformation<T, String> entityInformation,
                       MongoOperations mongoOperations) {
        super(entityInformation, mongoOperations);
        this.mongoOperations = mongoOperations;
        this.entityClass = entityInformation.getJavaType();
    }

    protected Page<T> pageableQuery(Query query, Pageable pageable) {
        Query countQuery = Query.of(query).limit(-1).skip(-1);

        long total = mongoOperations.count(countQuery, entityClass);

        query.with(pageable);
        List<T> list = mongoOperations.find(query, entityClass);

        return new PageImpl<>(list, pageable, total);
    }
}
