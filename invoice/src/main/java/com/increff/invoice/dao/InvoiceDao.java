package com.increff.invoice.dao;

import com.increff.invoice.db.InvoicePojo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceDao extends MongoRepository<InvoicePojo, String> {
    InvoicePojo findByOrderReferenceId(String orderReferenceId);
}
