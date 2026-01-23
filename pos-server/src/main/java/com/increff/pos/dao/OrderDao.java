package com.increff.pos.dao;

import com.increff.pos.db.OrderPojo;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderDao extends MongoRepository<OrderPojo, String> {
    OrderPojo findByOrderReferenceId(String orderReferenceId);
}
