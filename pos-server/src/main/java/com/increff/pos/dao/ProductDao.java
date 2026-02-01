package com.increff.pos.dao;

import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.form.ProductFilterForm;
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

@Repository
public class ProductDao extends AbstractDao<ProductPojo> {
    public ProductDao(MongoOperations mongoOperations) {
        super(new MongoRepositoryFactory(mongoOperations)
                .getEntityInformation(ProductPojo.class), mongoOperations);
    }

    public ProductPojo findByBarcode(String barcode) {
        Query q = Query.query(Criteria.where("barcode").is(barcode));
        return mongoOperations.findOne(q, ProductPojo.class);
    }

    public List<ProductPojo> findByBarcodes(List<String> barcodes) {
        Query q = Query.query(Criteria.where("barcode").in(barcodes));
        return mongoOperations.find(q, ProductPojo.class);
    }

    public Page<ProductPojo> filter(ProductFilterForm form, List<String> clientEmails) {
        List<Criteria> list = new ArrayList<>();

        if (form.getBarcode() != null && !form.getBarcode().isBlank()) {
            list.add(Criteria.where("barcode").regex(form.getBarcode(), "i"));
        }
        if (form.getName() != null && !form.getName().isBlank()) {
            list.add(Criteria.where("name").regex(form.getName(), "i"));
        }
        if (clientEmails != null && !clientEmails.isEmpty()) {
            list.add(Criteria.where("clientEmail").in(clientEmails));
        }

        Query q = new Query();
        if (!list.isEmpty()) {
            q.addCriteria(new Criteria().andOperator(list));
        }

        // ✅ Ensure same ordering as getAllProducts(): latest first
        Pageable p = PageRequest.of(
                form.getPage(),
                form.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return pageableQuery(q, p);
    }
}
