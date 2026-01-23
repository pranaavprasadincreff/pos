package com.increff.pos.dao;

import com.increff.pos.db.InventoryPojo;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryDao extends AbstractDao<InventoryPojo> {

    public InventoryDao(MongoOperations mongoOperations) {
        super(
                new MongoRepositoryFactory(mongoOperations)
                        .getEntityInformation(InventoryPojo.class),
                mongoOperations
        );
    }

    public InventoryPojo findByProductId(String productId) {
        Query query = Query.query(Criteria.where("productId").is(productId));
        return mongoOperations.findOne(query, InventoryPojo.class);
    }

    public boolean deductInventoryAtomically(String productId, int quantity) {
        Query query = Query.query(
                Criteria.where("productId").is(productId)
                        .and("quantity").gte(quantity)
        );
        Update update = new Update().inc("quantity", -quantity);
        InventoryPojo updated = mongoOperations.findAndModify(
                query,
                update,
                InventoryPojo.class
        );
        return updated != null;
    }
}
