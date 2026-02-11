package com.increff.pos.dao;

import com.increff.pos.db.DayToDaySalesReportPojo;
import com.increff.pos.db.subdocument.SalesReportClientBlock;
import com.increff.pos.db.subdocument.SalesReportProductBlock;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.constants.ReportRowType;
import com.increff.pos.model.data.SalesReportRowData;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
public class SalesReportDao {

    private static final ZoneId IST_TIMEZONE = ZoneId.of("Asia/Kolkata");

    private static final String DAILY_REPORT_COLLECTION = "sales_report_daily";
    private static final String ORDERS_COLLECTION = "orders";
    private static final String PRODUCTS_COLLECTION = "products";

    // Orders now use createdAt instead of orderTime
    private static final String ORDER_CREATED_AT_FIELD = "createdAt";

    private static final String ORDER_ITEMS_PATH = "orderItems";
    private static final String ORDER_ITEMS_PRODUCT_ID_PATH = "orderItems.productId";
    private static final String ORDER_ITEMS_PRODUCT_OID_PATH = "orderItems.productObjectId";

    private static final String PRODUCT_ID_FIELD = "_id";
    private static final String PRODUCT_LOOKUP_ALIAS = "product";
    private static final String PRODUCT_CLIENT_EMAIL_PATH = "product.clientEmail";
    private static final String PRODUCT_BARCODE_PATH = "product.barcode";

    private final MongoOperations mongoOperations;

    public SalesReportDao(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    // -------------------- Daily: stored doc --------------------

    public boolean existsDailyReportDocument(LocalDate reportDate) {
        if (reportDate == null) return false;
        Query query = Query.query(Criteria.where("_id").is(getDailyDocumentId(reportDate)));
        return mongoOperations.exists(query, DayToDaySalesReportPojo.class, DAILY_REPORT_COLLECTION);
    }

    public void generateAndStoreDailyReportDocument(LocalDate reportDate) {
        if (reportDate == null) return;
        DayToDaySalesReportPojo reportDocument = buildDailyReportDocumentFromOrders(reportDate);
        saveDailyReportDocument(reportDocument);
    }

    public List<SalesReportRowData> getDailyReportRows(LocalDate reportDate, String clientEmail, ReportRowType rowType) {
        if (reportDate == null) return List.of();
        DayToDaySalesReportPojo reportDocument = fetchDailyReportDocument(reportDate); // fetch only; no create
        return mapDailyDocumentToRows(reportDocument, clientEmail, rowType);
    }

    // -------------------- Range: LIVE from orders (NO writes) --------------------

    public List<SalesReportRowData> getRangeReportRows(
            LocalDate startDate,
            LocalDate endDate,
            String clientEmail,
            ReportRowType rowType
    ) {
        if (startDate == null || endDate == null) return List.of();

        if (rowType == ReportRowType.CLIENT) {
            return aggregateRangeClientRowsFromOrders(startDate, endDate, OrderStatus.INVOICED);
        }

        if (!StringUtils.hasText(clientEmail)) {
            return List.of();
        }

        return aggregateRangeProductRowsForClientFromOrders(startDate, endDate, clientEmail, OrderStatus.INVOICED);
    }

    // -------------------- Daily doc IO --------------------

    private DayToDaySalesReportPojo fetchDailyReportDocument(LocalDate reportDate) {
        Query query = Query.query(Criteria.where("_id").is(getDailyDocumentId(reportDate)));
        return mongoOperations.findOne(query, DayToDaySalesReportPojo.class, DAILY_REPORT_COLLECTION);
    }

    private void saveDailyReportDocument(DayToDaySalesReportPojo reportDocument) {
        if (reportDocument == null) return;
        mongoOperations.save(reportDocument, DAILY_REPORT_COLLECTION);
    }

    private String getDailyDocumentId(LocalDate reportDate) {
        // daily doc id = ISO date string (matches your code + existing DB usage)
        return reportDate.toString();
    }

    // -------------------- Core aggregation helpers --------------------

    private AddFieldsOperation addProductObjectIdField() {
        AggregationExpression toObjectIdExpr =
                context -> new org.bson.Document("$toObjectId", "$" + ORDER_ITEMS_PRODUCT_ID_PATH);

        return AddFieldsOperation.addField(ORDER_ITEMS_PRODUCT_OID_PATH)
                .withValue(toObjectIdExpr)
                .build();
    }

    private MatchOperation matchHasProductId() {
        // Avoid $toObjectId on null
        return match(Criteria.where(ORDER_ITEMS_PRODUCT_ID_PATH).ne(null));
    }

    private LookupOperation lookupProductByObjectId() {
        return LookupOperation.newLookup()
                .from(PRODUCTS_COLLECTION)
                .localField(ORDER_ITEMS_PRODUCT_OID_PATH)
                .foreignField(PRODUCT_ID_FIELD)
                .as(PRODUCT_LOOKUP_ALIAS);
    }

    // -------------------- Daily aggregation (orders -> nested doc) --------------------

    private DayToDaySalesReportPojo buildDailyReportDocumentFromOrders(LocalDate reportDate) {
        if (reportDate == null) return null;

        Aggregation aggregation = buildNestedDailyAggregation(reportDate, OrderStatus.INVOICED);

        AggregationResults<ClientAgg> results =
                mongoOperations.aggregate(aggregation, ORDERS_COLLECTION, ClientAgg.class);

        DayToDaySalesReportPojo reportDocument = new DayToDaySalesReportPojo();
        reportDocument.setId(reportDate.toString()); // _id equals date string
        reportDocument.setDate(reportDate);
        reportDocument.setClients(mapClientBlocks(results.getMappedResults()));
        return reportDocument;
    }

    private Aggregation buildNestedDailyAggregation(LocalDate reportDate, OrderStatus requiredOrderStatus) {
        ZonedDateTime start = reportDate.atStartOfDay(IST_TIMEZONE);
        ZonedDateTime endExclusive = reportDate.plusDays(1).atStartOfDay(IST_TIMEZONE);

        MatchOperation matchInvoicedOrders = match(new Criteria().andOperator(
                Criteria.where(ORDER_CREATED_AT_FIELD).gte(start).lt(endExclusive),
                Criteria.where("status").is(requiredOrderStatus.name())
        ));

        UnwindOperation unwindItems = unwind(ORDER_ITEMS_PATH);

        // join products by _id (ObjectId) using converted orderItems.productId
        LookupOperation lookupProduct = lookupProductByObjectId();
        UnwindOperation unwindProduct = unwind(PRODUCT_LOOKUP_ALIAS);

        AggregationExpression lineRevenue =
                ArithmeticOperators.Multiply.valueOf("orderItems.orderedQuantity")
                        .multiplyBy("orderItems.sellingPrice");

        // group by client + productBarcode (from looked-up product)
        GroupOperation groupByClientAndProduct = group(
                Fields.from(
                        Fields.field("clientEmail", PRODUCT_CLIENT_EMAIL_PATH),
                        Fields.field("productBarcode", PRODUCT_BARCODE_PATH)
                ))
                .addToSet("orderReferenceId").as("orderRefs")
                .sum("orderItems.orderedQuantity").as("itemsCount")
                .sum(lineRevenue).as("totalRevenue");

        ProjectionOperation projectClientProductRow = project()
                .and("_id.clientEmail").as("clientEmail")
                .and("_id.productBarcode").as("productBarcode")
                .and(ArrayOperators.Size.lengthOfArray("orderRefs")).as("ordersCount")
                .and("itemsCount").as("itemsCount")
                .and("totalRevenue").as("totalRevenue")
                .and("orderRefs").as("orderRefs");

        GroupOperation groupProductsUnderClient = group("clientEmail")
                .push(new Document("productBarcode", "$productBarcode")
                        .append("ordersCount", "$ordersCount")
                        .append("itemsCount", "$itemsCount")
                        .append("totalRevenue", "$totalRevenue"))
                .as("products")
                .addToSet("orderRefs").as("orderRefsArrays")
                .sum("itemsCount").as("itemsCount")
                .sum("totalRevenue").as("totalRevenue");

        AggregationExpression mergedOrderRefs =
                ArrayOperators.Reduce.arrayOf("orderRefsArrays")
                        .withInitialValue(List.of())
                        .reduce(SetOperators.SetUnion.arrayAsSet("$$value").union("$$this"));

        ProjectionOperation projectClientBlock = project()
                .and("_id").as("clientEmail")
                .and("products").as("products")
                .and(ArrayOperators.Size.lengthOfArray(mergedOrderRefs)).as("ordersCount")
                .and("itemsCount").as("itemsCount")
                .and("totalRevenue").as("totalRevenue");

        SortOperation sortClientsByRevenue = sort(Sort.by(Sort.Direction.DESC, "totalRevenue"));

        return newAggregation(
                matchInvoicedOrders,
                unwindItems,
                matchHasProductId(),
                addProductObjectIdField(),
                lookupProduct,
                unwindProduct,
                groupByClientAndProduct,
                projectClientProductRow,
                groupProductsUnderClient,
                projectClientBlock,
                sortClientsByRevenue
        );
    }

    private List<SalesReportClientBlock> mapClientBlocks(List<ClientAgg> clientAggs) {
        List<SalesReportClientBlock> clients = new ArrayList<>();
        if (clientAggs == null) return clients;

        for (ClientAgg clientAgg : clientAggs) {
            if (clientAgg == null) continue;

            SalesReportClientBlock client = new SalesReportClientBlock();
            client.setClientEmail(clientAgg.getClientEmail());
            client.setOrdersCount(nullSafeLongObj(clientAgg.getOrdersCount()));
            client.setItemsCount(nullSafeLongObj(clientAgg.getItemsCount()));
            client.setTotalRevenue(nullSafeDoubleObj(clientAgg.getTotalRevenue()));
            client.setProducts(mapProductBlocks(clientAgg.getProducts()));
            clients.add(client);
        }

        return clients;
    }

    private List<SalesReportProductBlock> mapProductBlocks(List<ProductAgg> productAggs) {
        List<SalesReportProductBlock> products = new ArrayList<>();
        if (productAggs == null) return products;

        for (ProductAgg productAgg : productAggs) {
            if (productAgg == null) continue;

            SalesReportProductBlock product = new SalesReportProductBlock();
            product.setProductBarcode(productAgg.getProductBarcode());
            product.setOrdersCount(nullSafeLongObj(productAgg.getOrdersCount()));
            product.setItemsCount(nullSafeLongObj(productAgg.getItemsCount()));
            product.setTotalRevenue(nullSafeDoubleObj(productAgg.getTotalRevenue()));
            products.add(product);
        }

        return products;
    }

    // -------------------- Range aggregations (orders -> rows) --------------------

    private List<SalesReportRowData> aggregateRangeClientRowsFromOrders(
            LocalDate startDate,
            LocalDate endDate,
            OrderStatus requiredOrderStatus
    ) {
        ZonedDateTime start = startDate.atStartOfDay(IST_TIMEZONE);
        ZonedDateTime endExclusive = endDate.plusDays(1).atStartOfDay(IST_TIMEZONE);

        MatchOperation matchOrders = match(new Criteria().andOperator(
                Criteria.where(ORDER_CREATED_AT_FIELD).gte(start).lt(endExclusive),
                Criteria.where("status").is(requiredOrderStatus.name())
        ));

        UnwindOperation unwindItems = unwind(ORDER_ITEMS_PATH);
        LookupOperation lookupProduct = lookupProductByObjectId();
        UnwindOperation unwindProduct = unwind(PRODUCT_LOOKUP_ALIAS);

        AggregationExpression lineRevenue =
                ArithmeticOperators.Multiply.valueOf("orderItems.orderedQuantity")
                        .multiplyBy("orderItems.sellingPrice");

        GroupOperation groupByClient = group(PRODUCT_CLIENT_EMAIL_PATH)
                .addToSet("orderReferenceId").as("orderRefs")
                .sum("orderItems.orderedQuantity").as("itemsCount")
                .sum(lineRevenue).as("totalRevenue");

        ProjectionOperation projectRow = project()
                .and("_id").as("clientEmail")
                .andExclude("_id")
                .and(ArrayOperators.Size.lengthOfArray("orderRefs")).as("ordersCount")
                .and("itemsCount").as("itemsCount")
                .and("totalRevenue").as("totalRevenue");

        SortOperation sortByRevenue = sort(Sort.by(Sort.Direction.DESC, "totalRevenue"));

        Aggregation agg = newAggregation(
                matchOrders,
                unwindItems,
                matchHasProductId(),
                addProductObjectIdField(),
                lookupProduct,
                unwindProduct,
                groupByClient,
                projectRow,
                sortByRevenue
        );

        AggregationResults<RangeClientRowAgg> results =
                mongoOperations.aggregate(agg, ORDERS_COLLECTION, RangeClientRowAgg.class);

        return mapRangeClientAggToRows(results.getMappedResults());
    }

    private List<SalesReportRowData> aggregateRangeProductRowsForClientFromOrders(
            LocalDate startDate,
            LocalDate endDate,
            String clientEmail,
            OrderStatus requiredOrderStatus
    ) {
        ZonedDateTime start = startDate.atStartOfDay(IST_TIMEZONE);
        ZonedDateTime endExclusive = endDate.plusDays(1).atStartOfDay(IST_TIMEZONE);

        MatchOperation matchOrders = match(new Criteria().andOperator(
                Criteria.where(ORDER_CREATED_AT_FIELD).gte(start).lt(endExclusive),
                Criteria.where("status").is(requiredOrderStatus.name())
        ));

        UnwindOperation unwindItems = unwind(ORDER_ITEMS_PATH);
        LookupOperation lookupProduct = lookupProductByObjectId();
        UnwindOperation unwindProduct = unwind(PRODUCT_LOOKUP_ALIAS);

        MatchOperation matchClient = match(Criteria.where(PRODUCT_CLIENT_EMAIL_PATH).is(clientEmail));

        AggregationExpression lineRevenue =
                ArithmeticOperators.Multiply.valueOf("orderItems.orderedQuantity")
                        .multiplyBy("orderItems.sellingPrice");

        // group by product barcode from looked-up product
        GroupOperation groupByBarcode = group(PRODUCT_BARCODE_PATH)
                .addToSet("orderReferenceId").as("orderRefs")
                .sum("orderItems.orderedQuantity").as("itemsCount")
                .sum(lineRevenue).as("totalRevenue");

        // inject constant clientEmail via $literal
        AggregationExpression constantClientEmail =
                context -> new org.bson.Document("$literal", clientEmail);

        ProjectionOperation projectRow = project()
                .and("_id").as("productBarcode")
                .andExclude("_id")
                .and(ArrayOperators.Size.lengthOfArray("orderRefs")).as("ordersCount")
                .and("itemsCount").as("itemsCount")
                .and("totalRevenue").as("totalRevenue")
                .and(constantClientEmail).as("clientEmail");

        SortOperation sortByRevenue = sort(Sort.by(Sort.Direction.DESC, "totalRevenue"));

        Aggregation agg = newAggregation(
                matchOrders,
                unwindItems,
                matchHasProductId(),
                addProductObjectIdField(),
                lookupProduct,
                unwindProduct,
                matchClient,
                groupByBarcode,
                projectRow,
                sortByRevenue
        );

        AggregationResults<RangeProductRowAgg> results =
                mongoOperations.aggregate(agg, ORDERS_COLLECTION, RangeProductRowAgg.class);

        return mapRangeProductAggToRows(results.getMappedResults());
    }

    private List<SalesReportRowData> mapRangeClientAggToRows(List<RangeClientRowAgg> aggs) {
        if (aggs == null) return List.of();
        List<SalesReportRowData> out = new ArrayList<>();

        for (RangeClientRowAgg a : aggs) {
            if (a == null) continue;
            SalesReportRowData r = new SalesReportRowData();
            r.setClientEmail(a.getClientEmail());
            r.setProductBarcode(null);
            r.setOrdersCount(nullSafeLong(a.getOrdersCount()));
            r.setItemsCount(nullSafeLong(a.getItemsCount()));
            r.setTotalRevenue(nullSafeDouble(a.getTotalRevenue()));
            out.add(r);
        }

        return out;
    }

    private List<SalesReportRowData> mapRangeProductAggToRows(List<RangeProductRowAgg> aggs) {
        if (aggs == null) return List.of();
        List<SalesReportRowData> out = new ArrayList<>();

        for (RangeProductRowAgg a : aggs) {
            if (a == null) continue;
            SalesReportRowData r = new SalesReportRowData();
            r.setClientEmail(a.getClientEmail());
            r.setProductBarcode(a.getProductBarcode());
            r.setOrdersCount(nullSafeLong(a.getOrdersCount()));
            r.setItemsCount(nullSafeLong(a.getItemsCount()));
            r.setTotalRevenue(nullSafeDouble(a.getTotalRevenue()));
            out.add(r);
        }

        return out;
    }

    // -------------------- Daily mapping (doc -> rows) --------------------

    private List<SalesReportRowData> mapDailyDocumentToRows(
            DayToDaySalesReportPojo reportDocument,
            String clientEmail,
            ReportRowType rowType
    ) {
        if (reportDocument == null || reportDocument.getClients() == null) {
            return List.of();
        }

        if (rowType == ReportRowType.CLIENT) {
            return mapClientRows(reportDocument.getClients());
        }

        return mapProductRowsForClient(reportDocument.getClients(), clientEmail);
    }

    private List<SalesReportRowData> mapClientRows(List<SalesReportClientBlock> clientBlocks) {
        List<SalesReportRowData> rows = new ArrayList<>();
        if (clientBlocks == null) return rows;

        for (SalesReportClientBlock clientBlock : clientBlocks) {
            if (clientBlock == null) continue;

            SalesReportRowData row = new SalesReportRowData();
            row.setClientEmail(clientBlock.getClientEmail());
            row.setProductBarcode(null);
            row.setOrdersCount(nullSafeLong(clientBlock.getOrdersCount()));
            row.setItemsCount(nullSafeLong(clientBlock.getItemsCount()));
            row.setTotalRevenue(nullSafeDouble(clientBlock.getTotalRevenue()));
            rows.add(row);
        }

        rows.sort(Comparator.comparingDouble(SalesReportRowData::getTotalRevenue).reversed());
        return rows;
    }

    private List<SalesReportRowData> mapProductRowsForClient(
            List<SalesReportClientBlock> clientBlocks,
            String clientEmail
    ) {
        if (!StringUtils.hasText(clientEmail)) return List.of();

        SalesReportClientBlock clientBlock = findClientBlock(clientBlocks, clientEmail);
        if (clientBlock == null || clientBlock.getProducts() == null) return List.of();

        List<SalesReportRowData> rows = new ArrayList<>();
        for (SalesReportProductBlock productBlock : clientBlock.getProducts()) {
            if (productBlock == null) continue;

            SalesReportRowData row = new SalesReportRowData();
            row.setClientEmail(clientBlock.getClientEmail());
            row.setProductBarcode(productBlock.getProductBarcode());
            row.setOrdersCount(nullSafeLong(productBlock.getOrdersCount()));
            row.setItemsCount(nullSafeLong(productBlock.getItemsCount()));
            row.setTotalRevenue(nullSafeDouble(productBlock.getTotalRevenue()));
            rows.add(row);
        }

        rows.sort(Comparator.comparingDouble(SalesReportRowData::getTotalRevenue).reversed());
        return rows;
    }

    private SalesReportClientBlock findClientBlock(
            List<SalesReportClientBlock> clientBlocks,
            String clientEmail
    ) {
        if (clientBlocks == null || !StringUtils.hasText(clientEmail)) return null;

        for (SalesReportClientBlock clientBlock : clientBlocks) {
            if (clientBlock == null) continue;
            if (clientEmail.equals(clientBlock.getClientEmail())) return clientBlock;
        }

        return null;
    }

    private long nullSafeLong(Long value) {
        return value == null ? 0L : value;
    }

    private double nullSafeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private Long nullSafeLongObj(Long value) {
        return value == null ? 0L : value;
    }

    private Double nullSafeDoubleObj(Double value) {
        return value == null ? 0.0 : value;
    }

    // -------------------- Aggregation mapping classes --------------------

    public static class ClientAgg {
        private String clientEmail;
        private Long ordersCount;
        private Long itemsCount;
        private Double totalRevenue;
        private List<ProductAgg> products;

        public String getClientEmail() { return clientEmail; }
        public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
        public Long getOrdersCount() { return ordersCount; }
        public void setOrdersCount(Long ordersCount) { this.ordersCount = ordersCount; }
        public Long getItemsCount() { return itemsCount; }
        public void setItemsCount(Long itemsCount) { this.itemsCount = itemsCount; }
        public Double getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }
        public List<ProductAgg> getProducts() { return products; }
        public void setProducts(List<ProductAgg> products) { this.products = products; }
    }

    public static class ProductAgg {
        private String productBarcode;
        private Long ordersCount;
        private Long itemsCount;
        private Double totalRevenue;

        public String getProductBarcode() { return productBarcode; }
        public void setProductBarcode(String productBarcode) { this.productBarcode = productBarcode; }
        public Long getOrdersCount() { return ordersCount; }
        public void setOrdersCount(Long ordersCount) { this.ordersCount = ordersCount; }
        public Long getItemsCount() { return itemsCount; }
        public void setItemsCount(Long itemsCount) { this.itemsCount = itemsCount; }
        public Double getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }
    }

    public static class RangeClientRowAgg {
        private String clientEmail;
        private Long ordersCount;
        private Long itemsCount;
        private Double totalRevenue;

        public String getClientEmail() { return clientEmail; }
        public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
        public Long getOrdersCount() { return ordersCount; }
        public void setOrdersCount(Long ordersCount) { this.ordersCount = ordersCount; }
        public Long getItemsCount() { return itemsCount; }
        public void setItemsCount(Long itemsCount) { this.itemsCount = itemsCount; }
        public Double getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }
    }

    public static class RangeProductRowAgg {
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
}
