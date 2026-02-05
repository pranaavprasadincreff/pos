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

    public Page<ProductPojo> filter(ProductFilterForm filterForm, List<String> clientEmails) {
        Query query = buildFilterQuery(filterForm, clientEmails);
        Pageable pageable = buildPageRequest(filterForm);
        return pageableQuery(query, pageable);
    }

    // -------------------- private helpers --------------------

    private Query buildFilterQuery(ProductFilterForm filterForm, List<String> clientEmails) {
        List<Criteria> criteria = new ArrayList<>();

        if (StringUtils.hasText(filterForm.getBarcode())) {
            criteria.add(buildContainsIgnoreCaseCriteria("barcode", filterForm.getBarcode()));
        }

        if (StringUtils.hasText(filterForm.getName())) {
            criteria.add(buildContainsIgnoreCaseCriteria("name", filterForm.getName()));
        }

        if (clientEmails != null && !clientEmails.isEmpty()) {
            criteria.add(Criteria.where("clientEmail").in(clientEmails));
        }

        Query query = new Query();
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria));
        }

        return query;
    }

    private Pageable buildPageRequest(ProductFilterForm filterForm) {
        return PageRequest.of(
                filterForm.getPage(),
                filterForm.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    private Criteria buildContainsIgnoreCaseCriteria(String fieldName, String rawValue) {
        String safeValue = Pattern.quote(rawValue.trim());
        Pattern pattern = Pattern.compile(".*" + safeValue + ".*", Pattern.CASE_INSENSITIVE);
        return Criteria.where(fieldName).regex(pattern);
    }
}
