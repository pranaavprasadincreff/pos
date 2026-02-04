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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Repository
public class ProductDao extends AbstractDao<ProductPojo> {

    public ProductDao(MongoOperations mongoOperations) {
        super(
                new MongoRepositoryFactory(mongoOperations).getEntityInformation(ProductPojo.class),
                mongoOperations
        );
    }

    public ProductPojo findByBarcode(String barcode) {
        Query query = Query.query(Criteria.where("barcode").is(barcode));
        return mongoOperations.findOne(query, ProductPojo.class);
    }

    public List<ProductPojo> findByBarcodes(List<String> barcodes) {
        if (barcodes == null || barcodes.isEmpty()) {
            return List.of();
        }
        Query query = Query.query(Criteria.where("barcode").in(barcodes));
        return mongoOperations.find(query, ProductPojo.class);
    }

    public Page<ProductPojo> filter(ProductFilterForm form, List<String> clientEmails) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (StringUtils.hasText(form.getBarcode())) {
            criteriaList.add(regexContainsIgnoreCase("barcode", form.getBarcode()));
        }
        if (StringUtils.hasText(form.getName())) {
            criteriaList.add(regexContainsIgnoreCase("name", form.getName()));
        }
        if (clientEmails != null && !clientEmails.isEmpty()) {
            criteriaList.add(Criteria.where("clientEmail").in(clientEmails));
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList));
        }

        Pageable pageable = PageRequest.of(
                form.getPage(),
                form.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return pageableQuery(query, pageable);
    }

    private Criteria regexContainsIgnoreCase(String fieldName, String rawValue) {
        String safe = Pattern.quote(rawValue.trim());
        Pattern pattern = Pattern.compile(".*" + safe + ".*", Pattern.CASE_INSENSITIVE);
        return Criteria.where(fieldName).regex(pattern);
    }
}
