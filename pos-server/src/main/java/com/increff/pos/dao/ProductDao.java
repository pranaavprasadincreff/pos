package com.increff.pos.dao;

import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.form.ProductSearchForm;
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

    public Page<ProductPojo> search(ProductSearchForm searchForm, List<String> clientEmails) {
        List<Criteria> criteria = new ArrayList<>();

        if (StringUtils.hasText(searchForm.getBarcode())) {
            criteria.add(buildPartialMatchingIgnoreCaseCriteria("barcode", searchForm.getBarcode()));
        }
        if (StringUtils.hasText(searchForm.getName())) {
            criteria.add(buildPartialMatchingIgnoreCaseCriteria("name", searchForm.getName()));
        }
        if (clientEmails != null && !clientEmails.isEmpty()) {
            criteria.add(Criteria.where("clientEmail").in(clientEmails));
        }

        Query query = new Query();
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria));
        }

        Pageable pageable = PageRequest.of(
                searchForm.getPage(),
                searchForm.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return pageableQuery(query, pageable);
    }

    public List<ProductPojo> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Query query = Query.query(Criteria.where("id").in(ids));
        return mongoOperations.find(query, ProductPojo.class);
    }

    private Criteria buildPartialMatchingIgnoreCaseCriteria(String fieldName, String rawValue) {
        String safeValue = Pattern.quote(rawValue.trim());
        Pattern pattern = Pattern.compile(".*" + safeValue + ".*", Pattern.CASE_INSENSITIVE);
        return Criteria.where(fieldName).regex(pattern);
    }
}
