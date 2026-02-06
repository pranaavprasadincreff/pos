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

    public boolean deductInventoryAtomically(String productId, int quantity) {
        Query query = Query.query(
                Criteria.where("productId").is(productId)
                        .and("quantity").gte(quantity)
        );

        Update update = new Update().inc("quantity", -quantity);

        InventoryPojo updated = mongoOperations.findAndModify(query, update, InventoryPojo.class);
        return updated != null;
    }

    public boolean deductInventoryBulk(Map<String, Integer> quantityToDeductByProductId) {
        BulkOperations bulkOps = mongoOperations.bulkOps(BulkOperations.BulkMode.ORDERED, InventoryPojo.class);

        for (Map.Entry<String, Integer> entry : quantityToDeductByProductId.entrySet()) {
            String productId = entry.getKey();
            Integer quantityToDeduct = entry.getValue();

            Query query = Query.query(
                    Criteria.where("productId").is(productId)
                            .and("quantity").gte(quantityToDeduct)
            );

            Update update = new Update().inc("quantity", -quantityToDeduct);

            bulkOps.updateOne(query, update);
        }

        var result = bulkOps.execute();
        return result.getModifiedCount() == quantityToDeductByProductId.size();
    }

    public void incrementInventoryBulk(Map<String, Integer> quantityToAddByProductId) {
        BulkOperations bulkOps = mongoOperations.bulkOps(BulkOperations.BulkMode.ORDERED, InventoryPojo.class);

        for (Map.Entry<String, Integer> entry : quantityToAddByProductId.entrySet()) {
            String productId = entry.getKey();
            Integer quantityToAdd = entry.getValue();

            Query query = Query.query(Criteria.where("productId").is(productId));
            Update update = new Update().inc("quantity", quantityToAdd);

            bulkOps.updateOne(query, update);
        }

        bulkOps.execute();
    }
}
