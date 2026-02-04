package com.increff.pos.dao;

import com.increff.pos.db.OrderPojo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
                new MongoRepositoryFactory(mongoOperations).getEntityInformation(OrderPojo.class),
                mongoOperations
        );
    }

    public OrderPojo findByOrderReferenceId(String orderReferenceId) {
        Query query = Query.query(Criteria.where("orderReferenceId").is(orderReferenceId));
        return mongoOperations.findOne(query, OrderPojo.class);
    }

    public Page<OrderPojo> search(
            String refContains,
            String status,
            ZonedDateTime fromTime,
            ZonedDateTime toTime,
            int page,
            int size
    ) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (StringUtils.hasText(refContains)) {
            String safe = Pattern.quote(refContains.trim());
            Pattern pattern = Pattern.compile(".*" + safe + ".*", Pattern.CASE_INSENSITIVE);
            criteriaList.add(Criteria.where("orderReferenceId").regex(pattern));
        }

        if (StringUtils.hasText(status)) {
            criteriaList.add(Criteria.where("status").is(status.trim()));
        }

        if (fromTime != null) {
            criteriaList.add(Criteria.where("orderTime").gte(fromTime));
        }

        if (toTime != null) {
            criteriaList.add(Criteria.where("orderTime").lte(toTime));
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderTime"));
        return pageableQuery(query, pageable);
    }
}
