package com.increff.pos.dao;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.repository.support.SimpleMongoRepository;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;

public abstract class AbstractDao<T> extends SimpleMongoRepository<T, String> {
    protected final MongoOperations mongoOperations;

    public AbstractDao(MongoEntityInformation<T, String> entityInformation, MongoOperations mongoOperations) {
        super(entityInformation, mongoOperations);
        this.mongoOperations = mongoOperations;
    }

}