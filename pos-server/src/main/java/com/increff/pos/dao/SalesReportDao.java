package com.increff.pos.dao;

import com.increff.pos.db.SalesReportAggregatePojo;
import com.increff.pos.db.SalesReportRowPojo;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.constants.ReportRowType;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
public class SalesReportDao extends AbstractDao<SalesReportAggregatePojo> {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private static final String ORDERS_COLLECTION = "orders";
    private static final String PRODUCTS_COLLECTION = "products";
    private static final String PRODUCT_BARCODE_FIELD = "barcode";      // ProductPojo.barcode
    private static final String PRODUCT_CLIENT_EMAIL_PATH = "product.clientEmail"; // after lookup+unwind

    public SalesReportDao(MongoOperations mongoOperations) {
        super(
                new MongoRepositoryFactory(mongoOperations)
                        .getEntityInformation(SalesReportAggregatePojo.class),
                mongoOperations
        );
        ensureIndexes();
    }

    public void replaceDailyAggregates(LocalDate date, List<SalesReportAggregatePojo> rows) {
        Query delete = Query.query(Criteria.where("date").is(date));
        mongoOperations.remove(delete, SalesReportAggregatePojo.class);

        if (rows != null && !rows.isEmpty()) {
            mongoOperations.insert(rows, SalesReportAggregatePojo.class);
        }
    }

    public List<SalesReportRowPojo> fetchDaily(LocalDate date, String clientEmail, ReportRowType type) {
        List<Criteria> list = new ArrayList<>();
        list.add(Criteria.where("date").is(date));
        list.add(Criteria.where("type").is(type.name()));

        if (StringUtils.hasText(clientEmail)) {
            list.add(Criteria.where("clientEmail").is(clientEmail));
        }

        Query q = new Query(new Criteria().andOperator(list));
        q.with(Sort.by(Sort.Direction.DESC, "totalRevenue"));

        List<SalesReportAggregatePojo> docs = mongoOperations.find(q, SalesReportAggregatePojo.class);
        return docs.stream().map(this::toRowPojoFromAggregate).toList();
    }

    private SalesReportRowPojo toRowPojoFromAggregate(SalesReportAggregatePojo a) {
        SalesReportRowPojo p = new SalesReportRowPojo();
        p.setClientEmail(a.getClientEmail());
        p.setProductBarcode(a.getProductBarcode());
        p.setOrdersCount(a.getOrdersCount());
        p.setItemsCount(a.getItemsCount());
        p.setTotalRevenue(a.getTotalRevenue());
        return p;
    }

    public List<SalesReportAggregatePojo> buildDailyAggregatesFacet(LocalDate date) {
        ZonedDateTime from = date.atStartOfDay(IST);
        ZonedDateTime toExclusive = date.plusDays(1).atStartOfDay(IST);

        MatchOperation matchOrders = match(new Criteria().andOperator(
                Criteria.where("orderTime").gte(from).lt(toExclusive),
                Criteria.where("status").is(OrderStatus.INVOICED.name())
        ));

        UnwindOperation unwindItems = unwind("orderItems");

        LookupOperation lookupProduct = LookupOperation.newLookup()
                .from(PRODUCTS_COLLECTION)
                .localField("orderItems.productBarcode")
                .foreignField(PRODUCT_BARCODE_FIELD)
                .as("product");

        UnwindOperation unwindProduct = unwind("product");

        AggregationExpression lineRevenue =
                ArithmeticOperators.Multiply.valueOf("orderItems.orderedQuantity")
                        .multiplyBy("orderItems.sellingPrice");

        GroupOperation groupByClient = group(PRODUCT_CLIENT_EMAIL_PATH)
                .addToSet("orderReferenceId").as("orderRefs")
                .sum("orderItems.orderedQuantity").as("itemsCount")
                .sum(lineRevenue).as("totalRevenue");

        ProjectionOperation projClient = project()
                .and("_id").as("clientEmail")
                .and(ArrayOperators.Size.lengthOfArray("orderRefs")).as("ordersCount")
                .and("itemsCount").as("itemsCount")
                .and("totalRevenue").as("totalRevenue");

        GroupOperation groupByClientAndProduct = group(
                Fields.from(
                        Fields.field("clientEmail", PRODUCT_CLIENT_EMAIL_PATH),
                        Fields.field("productBarcode", "orderItems.productBarcode")
                ))
                .addToSet("orderReferenceId").as("orderRefs")
                .sum("orderItems.orderedQuantity").as("itemsCount")
                .sum(lineRevenue).as("totalRevenue");

        ProjectionOperation projProduct = project()
                .and("_id.clientEmail").as("clientEmail")
                .and("_id.productBarcode").as("productBarcode")
                .and(ArrayOperators.Size.lengthOfArray("orderRefs")).as("ordersCount")
                .and("itemsCount").as("itemsCount")
                .and("totalRevenue").as("totalRevenue");

        FacetOperation facet = facet(groupByClient, projClient).as("clientRows")
                .and(groupByClientAndProduct, projProduct).as("productRows");

        Aggregation agg = newAggregation(
                matchOrders,
                unwindItems,
                lookupProduct,
                unwindProduct,
                facet
        );

        AggregationResults<DailyFacetResult> res =
                mongoOperations.aggregate(agg, ORDERS_COLLECTION, DailyFacetResult.class);

        DailyFacetResult out = res.getUniqueMappedResult();
        if (out == null) return List.of();

        List<SalesReportAggregatePojo> docs = new ArrayList<>();

        if (out.getClientRows() != null) {
            for (AggRow r : out.getClientRows()) {
                docs.add(toAggregate(date, ReportRowType.CLIENT, r));
            }
        }
        if (out.getProductRows() != null) {
            for (AggRow r : out.getProductRows()) {
                docs.add(toAggregate(date, ReportRowType.PRODUCT, r));
            }
        }

        return docs;
    }

    private SalesReportAggregatePojo toAggregate(LocalDate date, ReportRowType type, AggRow row) {
        SalesReportAggregatePojo p = new SalesReportAggregatePojo();
        p.setDate(date);
        p.setType(type.name());
        p.setClientEmail(row.getClientEmail());
        p.setProductBarcode(row.getProductBarcode());
        p.setOrdersCount(nullSafeLong(row.getOrdersCount()));
        p.setItemsCount(nullSafeLong(row.getItemsCount()));
        p.setTotalRevenue(nullSafeDouble(row.getTotalRevenue()));
        return p;
    }

    public List<SalesReportRowPojo> computeFromOrdersForSingleDay(LocalDate date, String clientEmail, ReportRowType type) {
        ZonedDateTime from = date.atStartOfDay(IST);
        ZonedDateTime toExclusive = date.plusDays(1).atStartOfDay(IST);
        return computeFromOrders(from, toExclusive, clientEmail, type);
    }

    public List<SalesReportRowPojo> computeFromOrdersForRange(LocalDate start, LocalDate end, String clientEmail, ReportRowType type) {
        ZonedDateTime from = start.atStartOfDay(IST);
        ZonedDateTime toExclusive = end.plusDays(1).atStartOfDay(IST);
        return computeFromOrders(from, toExclusive, clientEmail, type);
    }

    private List<SalesReportRowPojo> computeFromOrders(
            ZonedDateTime from,
            ZonedDateTime toExclusive,
            String clientEmail,
            ReportRowType type
    ) {
        MatchOperation matchOrders = match(new Criteria().andOperator(
                Criteria.where("orderTime").gte(from).lt(toExclusive),
                Criteria.where("status").is(OrderStatus.INVOICED.name())
        ));

        UnwindOperation unwindItems = unwind("orderItems");

        LookupOperation lookupProduct = LookupOperation.newLookup()
                .from(PRODUCTS_COLLECTION)
                .localField("orderItems.productBarcode")
                .foreignField(PRODUCT_BARCODE_FIELD)
                .as("product");

        UnwindOperation unwindProduct = unwind("product");

        List<AggregationOperation> ops = new ArrayList<>();
        ops.add(matchOrders);
        ops.add(unwindItems);
        ops.add(lookupProduct);
        ops.add(unwindProduct);

        if (StringUtils.hasText(clientEmail)) {
            ops.add(match(Criteria.where(PRODUCT_CLIENT_EMAIL_PATH).is(clientEmail)));
        }

        AggregationExpression lineRevenue =
                ArithmeticOperators.Multiply.valueOf("orderItems.orderedQuantity")
                        .multiplyBy("orderItems.sellingPrice");

        GroupOperation group = (type == ReportRowType.CLIENT)
                ? group(PRODUCT_CLIENT_EMAIL_PATH)
                .addToSet("orderReferenceId").as("orderRefs")
                .sum("orderItems.orderedQuantity").as("itemsCount")
                .sum(lineRevenue).as("totalRevenue")
                : group("orderItems.productBarcode")
                .addToSet("orderReferenceId").as("orderRefs")
                .sum("orderItems.orderedQuantity").as("itemsCount")
                .sum(lineRevenue).as("totalRevenue")
                .first(PRODUCT_CLIENT_EMAIL_PATH).as("clientEmail");

        ops.add(group);

        ProjectionOperation project = (type == ReportRowType.CLIENT)
                ? project()
                .and("_id").as("clientEmail")
                .and(ArrayOperators.Size.lengthOfArray("orderRefs")).as("ordersCount")
                .and("itemsCount").as("itemsCount")
                .and("totalRevenue").as("totalRevenue")
                : project()
                .and("_id").as("productBarcode")
                .and("clientEmail").as("clientEmail")
                .and(ArrayOperators.Size.lengthOfArray("orderRefs")).as("ordersCount")
                .and("itemsCount").as("itemsCount")
                .and("totalRevenue").as("totalRevenue");

        ops.add(project);
        ops.add(sort(Sort.by(Sort.Direction.DESC, "totalRevenue")));

        Aggregation agg = newAggregation(ops);

        AggregationResults<AggRow> res =
                mongoOperations.aggregate(agg, ORDERS_COLLECTION, AggRow.class);

        return res.getMappedResults().stream().map(this::toRowPojo).toList();
    }

    private SalesReportRowPojo toRowPojo(AggRow r) {
        SalesReportRowPojo p = new SalesReportRowPojo();
        p.setClientEmail(r.getClientEmail());
        p.setProductBarcode(r.getProductBarcode());
        p.setOrdersCount(nullSafeLong(r.getOrdersCount()));
        p.setItemsCount(nullSafeLong(r.getItemsCount()));
        p.setTotalRevenue(nullSafeDouble(r.getTotalRevenue()));
        return p;
    }

    private long nullSafeLong(Long v) { return v == null ? 0L : v; }
    private double nullSafeDouble(Double v) { return v == null ? 0.0 : v; }

    public static class AggRow {
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

    public static class DailyFacetResult {
        private List<AggRow> clientRows;
        private List<AggRow> productRows;

        public List<AggRow> getClientRows() { return clientRows; }
        public void setClientRows(List<AggRow> clientRows) { this.clientRows = clientRows; }
        public List<AggRow> getProductRows() { return productRows; }
        public void setProductRows(List<AggRow> productRows) { this.productRows = productRows; }
    }

    private void ensureIndexes() {
        mongoOperations.indexOps(SalesReportAggregatePojo.class).ensureIndex(
                new Index()
                        .on("date", Sort.Direction.ASC)
                        .on("type", Sort.Direction.ASC)
                        .on("clientEmail", Sort.Direction.ASC)
                        .on("productBarcode", Sort.Direction.ASC)
        );
    }
}
