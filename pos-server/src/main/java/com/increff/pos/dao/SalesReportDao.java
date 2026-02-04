package com.increff.pos.dao;

import com.increff.pos.db.DayToDaySalesReportPojo;
import com.increff.pos.db.SalesReportRowPojo;
import com.increff.pos.helper.SalesReportAggregationHelper;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.constants.ReportRowType;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
public class SalesReportDao {

    private static final ZoneId IST_TIMEZONE = ZoneId.of("Asia/Kolkata");

    private static final String DAILY_REPORT_COLLECTION = "sales_report_daily";
    private static final String ORDERS_COLLECTION = "orders";
    private static final String PRODUCTS_COLLECTION = "products";
    private static final String PRODUCT_BARCODE_FIELD = "barcode";
    private static final String PRODUCT_CLIENT_EMAIL_PATH = "product.clientEmail";

    private final MongoOperations mongoOperations;

    public SalesReportDao(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    public boolean existsDailyReportDocument(LocalDate reportDate) {
        Query query = Query.query(Criteria.where("_id").is(toDocumentId(reportDate)));
        return mongoOperations.exists(query, DayToDaySalesReportPojo.class, DAILY_REPORT_COLLECTION);
    }

    public void generateAndStoreDailyReportDocument(LocalDate reportDate) {
        DayToDaySalesReportPojo reportDocument = buildDailyReportDocumentFromOrders(reportDate);
        saveDailyReportDocument(reportDocument);
    }

    public List<SalesReportRowPojo> getDailyReportRows(LocalDate reportDate, String clientEmail, ReportRowType rowType) {
        DayToDaySalesReportPojo reportDocument = fetchOrCreateDailyReportDocument(reportDate);
        return mapDailyDocumentToRows(reportDocument, clientEmail, rowType);
    }

    public List<SalesReportRowPojo> getRangeReportRows(
            LocalDate startDate,
            LocalDate endDate,
            String clientEmail,
            ReportRowType rowType
    ) {
        Map<LocalDate, DayToDaySalesReportPojo> existingDocuments = fetchDailyReportDocumentsInRange(startDate, endDate);
        List<DayToDaySalesReportPojo> completeDocuments = ensureMissingDailyDocuments(startDate, endDate, existingDocuments);

        if (rowType == ReportRowType.CLIENT) {
            return computeClientRangeRowsInMemory(completeDocuments);
        }

        return computeProductRangeRowsInMemory(completeDocuments, clientEmail);
    }

    // -------------------- Fetch / Create strategy --------------------

    private DayToDaySalesReportPojo fetchOrCreateDailyReportDocument(LocalDate reportDate) {
        DayToDaySalesReportPojo existingDocument = fetchDailyReportDocument(reportDate);
        if (existingDocument != null) return existingDocument;

        DayToDaySalesReportPojo generatedDocument = buildDailyReportDocumentFromOrders(reportDate);
        saveDailyReportDocument(generatedDocument);
        return generatedDocument;
    }

    private DayToDaySalesReportPojo fetchDailyReportDocument(LocalDate reportDate) {
        Query query = Query.query(Criteria.where("_id").is(toDocumentId(reportDate)));
        return mongoOperations.findOne(query, DayToDaySalesReportPojo.class, DAILY_REPORT_COLLECTION);
    }

    private void saveDailyReportDocument(DayToDaySalesReportPojo reportDocument) {
        mongoOperations.save(reportDocument, DAILY_REPORT_COLLECTION);
    }

    private Map<LocalDate, DayToDaySalesReportPojo> fetchDailyReportDocumentsInRange(LocalDate startDate, LocalDate endDate) {
        Query query = Query.query(Criteria.where("_id")
                .gte(toDocumentId(startDate))
                .lte(toDocumentId(endDate))
        );

        List<DayToDaySalesReportPojo> docs =
                mongoOperations.find(query, DayToDaySalesReportPojo.class, DAILY_REPORT_COLLECTION);

        Map<LocalDate, DayToDaySalesReportPojo> byDate = new HashMap<>();
        if (docs == null) return byDate;

        for (DayToDaySalesReportPojo doc : docs) {
            if (doc == null || doc.getDate() == null) continue;
            byDate.put(doc.getDate(), doc);
        }

        return byDate;
    }

    private List<DayToDaySalesReportPojo> ensureMissingDailyDocuments(
            LocalDate startDate,
            LocalDate endDate,
            Map<LocalDate, DayToDaySalesReportPojo> existingByDate
    ) {
        List<DayToDaySalesReportPojo> completeDocuments = new ArrayList<>();
        LocalDate cursor = startDate;

        while (!cursor.isAfter(endDate)) {
            DayToDaySalesReportPojo existing = existingByDate.get(cursor);
            if (existing != null) {
                completeDocuments.add(existing);
                cursor = cursor.plusDays(1);
                continue;
            }

            DayToDaySalesReportPojo generated = buildDailyReportDocumentFromOrders(cursor);
            saveDailyReportDocument(generated);
            completeDocuments.add(generated);

            cursor = cursor.plusDays(1);
        }

        return completeDocuments;
    }

    private DayToDaySalesReportPojo buildDailyReportDocumentFromOrders(LocalDate reportDate) {
        return SalesReportAggregationHelper.buildDailyNestedReportFromOrders(
                mongoOperations,
                reportDate,
                IST_TIMEZONE,
                ORDERS_COLLECTION,
                PRODUCTS_COLLECTION,
                PRODUCT_BARCODE_FIELD,
                PRODUCT_CLIENT_EMAIL_PATH,
                OrderStatus.INVOICED
        );
    }

    private String toDocumentId(LocalDate reportDate) {
        return reportDate.toString();
    }

    // -------------------- Daily mapping (doc -> rows) --------------------

    private List<SalesReportRowPojo> mapDailyDocumentToRows(
            DayToDaySalesReportPojo reportDocument,
            String clientEmail,
            ReportRowType rowType
    ) {
        if (reportDocument == null || reportDocument.getClients() == null) return List.of();

        if (rowType == ReportRowType.CLIENT) {
            return mapClientRows(reportDocument.getClients());
        }

        return mapProductRowsForClient(reportDocument.getClients(), clientEmail);
    }

    private List<SalesReportRowPojo> mapClientRows(List<DayToDaySalesReportPojo.ClientBlock> clients) {
        List<SalesReportRowPojo> rows = new ArrayList<>();

        for (DayToDaySalesReportPojo.ClientBlock client : clients) {
            if (client == null) continue;

            SalesReportRowPojo row = new SalesReportRowPojo();
            row.setClientEmail(client.getClientEmail());
            row.setProductBarcode(null);
            row.setOrdersCount(nullSafeLong(client.getOrdersCount()));
            row.setItemsCount(nullSafeLong(client.getItemsCount()));
            row.setTotalRevenue(nullSafeDouble(client.getTotalRevenue()));
            rows.add(row);
        }

        rows.sort(Comparator.comparingDouble(SalesReportRowPojo::getTotalRevenue).reversed());
        return rows;
    }

    private List<SalesReportRowPojo> mapProductRowsForClient(
            List<DayToDaySalesReportPojo.ClientBlock> clients,
            String clientEmail
    ) {
        if (!StringUtils.hasText(clientEmail)) return List.of();

        DayToDaySalesReportPojo.ClientBlock clientBlock = findClientBlock(clients, clientEmail);
        if (clientBlock == null || clientBlock.getProducts() == null) return List.of();

        List<SalesReportRowPojo> rows = new ArrayList<>();

        for (DayToDaySalesReportPojo.ProductBlock product : clientBlock.getProducts()) {
            if (product == null) continue;

            SalesReportRowPojo row = new SalesReportRowPojo();
            row.setClientEmail(clientBlock.getClientEmail());
            row.setProductBarcode(product.getProductBarcode());
            row.setOrdersCount(nullSafeLong(product.getOrdersCount()));
            row.setItemsCount(nullSafeLong(product.getItemsCount()));
            row.setTotalRevenue(nullSafeDouble(product.getTotalRevenue()));
            rows.add(row);
        }

        rows.sort(Comparator.comparingDouble(SalesReportRowPojo::getTotalRevenue).reversed());
        return rows;
    }

    private DayToDaySalesReportPojo.ClientBlock findClientBlock(
            List<DayToDaySalesReportPojo.ClientBlock> clients,
            String clientEmail
    ) {
        for (DayToDaySalesReportPojo.ClientBlock client : clients) {
            if (client == null) continue;
            if (clientEmail.equals(client.getClientEmail())) return client;
        }
        return null;
    }

    // -------------------- Range rows computed in-memory (minimizes DB calls) --------------------

    private List<SalesReportRowPojo> computeClientRangeRowsInMemory(List<DayToDaySalesReportPojo> documents) {
        Map<String, SalesReportRowPojo> byClient = new HashMap<>();

        for (DayToDaySalesReportPojo doc : documents) {
            if (doc == null || doc.getClients() == null) continue;

            for (DayToDaySalesReportPojo.ClientBlock client : doc.getClients()) {
                if (client == null || !StringUtils.hasText(client.getClientEmail())) continue;

                SalesReportRowPojo accumulator = byClient.computeIfAbsent(client.getClientEmail(), k -> createClientAccumulator(k));
                accumulator.setOrdersCount(accumulator.getOrdersCount() + nullSafeLong(client.getOrdersCount()));
                accumulator.setItemsCount(accumulator.getItemsCount() + nullSafeLong(client.getItemsCount()));
                accumulator.setTotalRevenue(accumulator.getTotalRevenue() + nullSafeDouble(client.getTotalRevenue()));
            }
        }

        List<SalesReportRowPojo> rows = new ArrayList<>(byClient.values());
        rows.sort(Comparator.comparingDouble(SalesReportRowPojo::getTotalRevenue).reversed());
        return rows;
    }

    private List<SalesReportRowPojo> computeProductRangeRowsInMemory(
            List<DayToDaySalesReportPojo> documents,
            String clientEmail
    ) {
        if (!StringUtils.hasText(clientEmail)) return List.of();

        Map<String, SalesReportRowPojo> byBarcode = new HashMap<>();

        for (DayToDaySalesReportPojo doc : documents) {
            if (doc == null || doc.getClients() == null) continue;

            DayToDaySalesReportPojo.ClientBlock client = findClientBlock(doc.getClients(), clientEmail);
            if (client == null || client.getProducts() == null) continue;

            for (DayToDaySalesReportPojo.ProductBlock product : client.getProducts()) {
                if (product == null || !StringUtils.hasText(product.getProductBarcode())) continue;

                SalesReportRowPojo accumulator = byBarcode.computeIfAbsent(
                        product.getProductBarcode(),
                        k -> createProductAccumulator(clientEmail, k)
                );

                accumulator.setOrdersCount(accumulator.getOrdersCount() + nullSafeLong(product.getOrdersCount()));
                accumulator.setItemsCount(accumulator.getItemsCount() + nullSafeLong(product.getItemsCount()));
                accumulator.setTotalRevenue(accumulator.getTotalRevenue() + nullSafeDouble(product.getTotalRevenue()));
            }
        }

        List<SalesReportRowPojo> rows = new ArrayList<>(byBarcode.values());
        rows.sort(Comparator.comparingDouble(SalesReportRowPojo::getTotalRevenue).reversed());
        return rows;
    }

    private SalesReportRowPojo createClientAccumulator(String clientEmail) {
        SalesReportRowPojo row = new SalesReportRowPojo();
        row.setClientEmail(clientEmail);
        row.setProductBarcode(null);
        row.setOrdersCount(0);
        row.setItemsCount(0);
        row.setTotalRevenue(0.0);
        return row;
    }

    private SalesReportRowPojo createProductAccumulator(String clientEmail, String productBarcode) {
        SalesReportRowPojo row = new SalesReportRowPojo();
        row.setClientEmail(clientEmail);
        row.setProductBarcode(productBarcode);
        row.setOrdersCount(0);
        row.setItemsCount(0);
        row.setTotalRevenue(0.0);
        return row;
    }

    private long nullSafeLong(Long v) { return v == null ? 0L : v; }
    private double nullSafeDouble(Double v) { return v == null ? 0.0 : v; }
}
