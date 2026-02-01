package com.increff.pos.dao;

import com.increff.pos.db.OrderPojo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Repository
public class OrderDao extends AbstractDao<OrderPojo> {
    public OrderDao(MongoOperations mongoOperations) {
        super(
                new MongoRepositoryFactory(mongoOperations)
                        .getEntityInformation(OrderPojo.class),
                mongoOperations
        );
    }

    public OrderPojo findByOrderReferenceId(String orderReferenceId) {
        Query q = Query.query(Criteria.where("orderReferenceId").is(orderReferenceId));
        return mongoOperations.findOne(q, OrderPojo.class);
    }

    public Page<OrderPojo> search(
            String refContains,
            String status,
            ZonedDateTime fromTime,
            ZonedDateTime toTime,
            int page,
            int size
    ) {
        List<Criteria> list = new ArrayList<>();
        if (StringUtils.hasText(refContains)) {
            String safe = Pattern.quote(refContains.trim());
            list.add(Criteria.where("orderReferenceId").regex(".*" + safe + ".*", "i"));
        }
        if (StringUtils.hasText(status)) {
            list.add(Criteria.where("status").is(status.trim()));
        }
        if (fromTime != null) {
            list.add(Criteria.where("orderTime").gte(fromTime));
        }
        if (toTime != null) {
            list.add(Criteria.where("orderTime").lte(toTime));
        }
        Query q = new Query();
        if (!list.isEmpty()) {
            q.addCriteria(new Criteria().andOperator(list));
        }
        Pageable p = PageRequest.of(page, size);
        return pageableQuery(q, p);
    }
}
