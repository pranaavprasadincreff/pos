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
import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
public class SalesReportDao {

    private static final ZoneId IST_TIMEZONE = ZoneId.of("Asia/Kolkata");

    private static final String ORDERS_COLLECTION = "orders";
    private static final String PRODUCTS_COLLECTION = "products";
    private static final String PRODUCT_BARCODE_FIELD = "barcode";
    private static final String PRODUCT_CLIENT_EMAIL_PATH = "product.clientEmail";

    public static final String NESTED_REPORT_COLLECTION = "sales_report_daily";

    private final MongoOperations mongoOperations;

    public SalesReportDao(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    // -------------------- Existence + generation --------------------

    public boolean existsDailyNestedReport(LocalDate reportDate) {
        Query query = Query.query(Criteria.where("_id").is(toReportDocumentId(reportDate)));
        return mongoOperations.exists(query, DayToDaySalesReportPojo.class, NESTED_REPORT_COLLECTION);
    }

    public void ensureDailyNestedReportExists(LocalDate reportDate) {
        if (existsDailyNestedReport(reportDate)) return;
        generateAndStoreDailyNestedReport(reportDate);
    }

    public void ensureDailyNestedReportsExistForRange(LocalDate startDate, LocalDate endDate) {
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            ensureDailyNestedReportExists(cursor);
            cursor = cursor.plusDays(1);
        }
    }

    public void generateAndStoreDailyNestedReport(LocalDate reportDate) {
        DayToDaySalesReportPojo reportDocument = buildDailyNestedReportFromOrders(reportDate);
        saveDailyNestedReport(reportDocument);
    }

    // -------------------- Reads (DAILY) --------------------

    public List<SalesReportRowPojo> fetchDailyRowsFromNestedReport(LocalDate reportDate, String clientEmail, ReportRowType rowType) {
        DayToDaySalesReportPojo reportDocument = fetchDailyNestedReport(reportDate);
        return mapDailyDocumentToRows(reportDocument, clientEmail, rowType);
    }

    // -------------------- Reads (RANGE) via aggregation on stored docs --------------------

    public List<SalesReportRowPojo> fetchRangeRowsFromNestedReports(
            LocalDate startDate,
            LocalDate endDate,
            String clientEmail,
            ReportRowType rowType
    ) {
        Aggregation aggregation = buildRangeAggregation(startDate, endDate, clientEmail, rowType);
        AggregationResults<RangeRowAgg> results =
                mongoOperations.aggregate(aggregation, NESTED_REPORT_COLLECTION, RangeRowAgg.class);

        return mapRangeAggregationResults(results.getMappedResults(), rowType);
    }

    // -------------------- Private persistence helpers --------------------

    private DayToDaySalesReportPojo fetchDailyNestedReport(LocalDate reportDate) {
        Query query = Query.query(Criteria.where("_id").is(toReportDocumentId(reportDate)));
        return mongoOperations.findOne(query, DayToDaySalesReportPojo.class, NESTED_REPORT_COLLECTION);
    }

    private void saveDailyNestedReport(DayToDaySalesReportPojo reportDocument) {
        mongoOperations.save(reportDocument, NESTED_REPORT_COLLECTION);
    }

    private DayToDaySalesReportPojo buildDailyNestedReportFromOrders(LocalDate reportDate) {
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

    private String toReportDocumentId(LocalDate reportDate) {
        return reportDate.toString(); // ISO yyyy-MM-dd
    }

    // -------------------- DAILY mapping --------------------

    private List<SalesReportRowPojo> mapDailyDocumentToRows(
            DayToDaySalesReportPojo reportDocument,
            String clientEmail,
            ReportRowType rowType
    ) {
        if (reportDocument == null) return List.of();

        if (rowType == ReportRowType.CLIENT) {
            return mapDailyClientRows(reportDocument);
        }

        return mapDailyProductRows(reportDocument, clientEmail);
    }

    private List<SalesReportRowPojo> mapDailyClientRows(DayToDaySalesReportPojo reportDocument) {
        List<SalesReportRowPojo> rows = new ArrayList<>();

        List<DayToDaySalesReportPojo.ClientBlock> clients = reportDocument.getClients();
        if (clients == null) return rows;

        for (DayToDaySalesReportPojo.ClientBlock client : clients) {
            SalesReportRowPojo row = new SalesReportRowPojo();
            row.setClientEmail(client.getClientEmail());
            row.setProductBarcode(null);
            row.setOrdersCount(nullSafeLong(client.getOrdersCount()));
            row.setItemsCount(nullSafeLong(client.getItemsCount()));
            row.setTotalRevenue(nullSafeDouble(client.getTotalRevenue()));
            rows.add(row);
        }

        return rows;
    }

    private List<SalesReportRowPojo> mapDailyProductRows(DayToDaySalesReportPojo reportDocument, String clientEmail) {
        DayToDaySalesReportPojo.ClientBlock clientBlock = findClientBlock(reportDocument, clientEmail);
        if (clientBlock == null) return List.of();

        List<SalesReportRowPojo> rows = new ArrayList<>();
        List<DayToDaySalesReportPojo.ProductBlock> products = clientBlock.getProducts();
        if (products == null) return rows;

        for (DayToDaySalesReportPojo.ProductBlock product : products) {
            SalesReportRowPojo row = new SalesReportRowPojo();
            row.setClientEmail(clientBlock.getClientEmail());
            row.setProductBarcode(product.getProductBarcode());
            row.setOrdersCount(nullSafeLong(product.getOrdersCount()));
            row.setItemsCount(nullSafeLong(product.getItemsCount()));
            row.setTotalRevenue(nullSafeDouble(product.getTotalRevenue()));
            rows.add(row);
        }

        return rows;
    }

    private DayToDaySalesReportPojo.ClientBlock findClientBlock(DayToDaySalesReportPojo reportDocument, String clientEmail) {
        if (!StringUtils.hasText(clientEmail)) return null;

        List<DayToDaySalesReportPojo.ClientBlock> clients = reportDocument.getClients();
        if (clients == null) return null;

        for (DayToDaySalesReportPojo.ClientBlock client : clients) {
            if (clientEmail.equals(client.getClientEmail())) return client;
        }

        return null;
    }

    // -------------------- RANGE aggregation on nested docs --------------------

    private Aggregation buildRangeAggregation(LocalDate startDate, LocalDate endDate, String clientEmail, ReportRowType rowType) {
        MatchOperation matchDateRange = match(Criteria.where("_id")
                .gte(startDate.toString())
                .lte(endDate.toString())
        );

        UnwindOperation unwindClients = unwind("clients");

        if (rowType == ReportRowType.CLIENT) {
            return buildClientRangeAggregation(matchDateRange, unwindClients);
        }

        return buildProductRangeAggregation(matchDateRange, unwindClients, clientEmail);
    }

    private Aggregation buildClientRangeAggregation(MatchOperation matchDateRange, UnwindOperation unwindClients) {
        GroupOperation groupByClientEmail = group("clients.clientEmail")
                .sum("clients.ordersCount").as("ordersCount")
                .sum("clients.itemsCount").as("itemsCount")
                .sum("clients.totalRevenue").as("totalRevenue");

        ProjectionOperation projectClientRow = project()
                .and("_id").as("clientEmail")
                .andExclude("_id")
                .and("ordersCount").as("ordersCount")
                .and("itemsCount").as("itemsCount")
                .and("totalRevenue").as("totalRevenue");

        SortOperation sortByRevenueDesc = sort(Sort.by(Sort.Direction.DESC, "totalRevenue"));

        return newAggregation(
                matchDateRange,
                unwindClients,
                groupByClientEmail,
                projectClientRow,
                sortByRevenueDesc
        );
    }

    private Aggregation buildProductRangeAggregation(MatchOperation matchDateRange, UnwindOperation unwindClients, String clientEmail) {
        MatchOperation matchClient = match(Criteria.where("clients.clientEmail").is(clientEmail));
        UnwindOperation unwindProducts = unwind("clients.products");

        GroupOperation groupByProductBarcode = group("clients.products.productBarcode")
                .sum("clients.products.ordersCount").as("ordersCount")
                .sum("clients.products.itemsCount").as("itemsCount")
                .sum("clients.products.totalRevenue").as("totalRevenue")
                .first("clients.clientEmail").as("clientEmail");

        ProjectionOperation projectProductRow = project()
                .and("clientEmail").as("clientEmail")
                .and("_id").as("productBarcode")
                .andExclude("_id")
                .and("ordersCount").as("ordersCount")
                .and("itemsCount").as("itemsCount")
                .and("totalRevenue").as("totalRevenue");

        SortOperation sortByRevenueDesc = sort(Sort.by(Sort.Direction.DESC, "totalRevenue"));

        return newAggregation(
                matchDateRange,
                unwindClients,
                matchClient,
                unwindProducts,
                groupByProductBarcode,
                projectProductRow,
                sortByRevenueDesc
        );
    }

    private List<SalesReportRowPojo> mapRangeAggregationResults(List<RangeRowAgg> results, ReportRowType rowType) {
        if (results == null) return List.of();

        List<SalesReportRowPojo> rows = new ArrayList<>();
        for (RangeRowAgg resultRow : results) {
            SalesReportRowPojo row = new SalesReportRowPojo();
            row.setClientEmail(resultRow.getClientEmail());
            row.setProductBarcode(rowType == ReportRowType.PRODUCT ? resultRow.getProductBarcode() : null);
            row.setOrdersCount(nullSafeLong(resultRow.getOrdersCount()));
            row.setItemsCount(nullSafeLong(resultRow.getItemsCount()));
            row.setTotalRevenue(nullSafeDouble(resultRow.getTotalRevenue()));
            rows.add(row);
        }

        return rows;
    }

    public static class RangeRowAgg {
        private String clientEmail;
        private String productBarcode;
        private Long ordersCount;
        private Long itemsCount;
        private Double totalRevenue;

        public String getClientEmail() { return clientEmail; }
        public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
        public String getProductBarcode() { return productBarcode; }
        public void setProductBarcode(String productBarcode) { this.productBarcode = productBarcode; }
        public Long getOrdersCount() { return ordersCount; }
        public void setOrdersCount(Long ordersCount) { this.ordersCount = ordersCount; }
        public Long getItemsCount() { return itemsCount; }
        public void setItemsCount(Long itemsCount) { this.itemsCount = itemsCount; }
        public Double getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }
    }

    private long nullSafeLong(Long v) { return v == null ? 0L : v; }
    private double nullSafeDouble(Double v) { return v == null ? 0.0 : v; }
}
