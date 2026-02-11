package com.increff.pos.dao;

import com.increff.pos.db.InventoryPojo;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class InventoryDao extends AbstractDao<InventoryPojo> {

    public InventoryDao(MongoOperations mongoOperations) {
        super(
                new MongoRepositoryFactory(mongoOperations).getEntityInformation(InventoryPojo.class),
                mongoOperations
        );
    }

    public InventoryPojo findByProductId(String productId) {
        Query query = Query.query(Criteria.where("productId").is(productId));
        return mongoOperations.findOne(query, InventoryPojo.class);
    }

    public List<InventoryPojo> findByProductIds(List<String> productIds) {
        Query query = Query.query(Criteria.where("productId").in(productIds));
        return mongoOperations.find(query, InventoryPojo.class);
    }

    public void deductInventoryBulk(Map<String, Integer> quantityByProductId) {
        BulkOperations bulkOps =
                mongoOperations.bulkOps(BulkOperations.BulkMode.ORDERED, InventoryPojo.class);

        for (var entry : quantityByProductId.entrySet()) {
            Query query = Query.query(
                    Criteria.where("productId").is(entry.getKey())
                            .and("quantity").gte(entry.getValue())
            );
            bulkOps.updateOne(query, new Update().inc("quantity", -entry.getValue()));
        }

        var result = bulkOps.execute();
        result.getModifiedCount();
    }


    public void incrementInventoryBulk(Map<String, Integer> quantityByProductId) {
        BulkOperations bulkOps = mongoOperations.bulkOps(BulkOperations.BulkMode.UNORDERED, InventoryPojo.class);

        for (Map.Entry<String, Integer> entry : quantityByProductId.entrySet()) {
            String productId = entry.getKey();
            int quantity = entry.getValue();

            Query query = Query.query(Criteria.where("productId").is(productId));
            Update update = new Update().inc("quantity", quantity);

            bulkOps.updateOne(query, update);
        }

        bulkOps.execute();
    }
}
