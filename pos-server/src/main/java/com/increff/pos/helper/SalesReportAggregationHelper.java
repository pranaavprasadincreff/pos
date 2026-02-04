package com.increff.pos.helper;

import com.increff.pos.db.DayToDaySalesReportPojo;
import com.increff.pos.model.constants.OrderStatus;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

public class SalesReportAggregationHelper {

    private SalesReportAggregationHelper() {}

    public static DayToDaySalesReportPojo buildDailyNestedReportFromOrders(
            MongoOperations mongoOperations,
            LocalDate reportDate,
            ZoneId timezone,
            String ordersCollection,
            String productsCollection,
            String productBarcodeField,
            String productClientEmailPath,
            OrderStatus requiredOrderStatus
    ) {
        Aggregation aggregation = buildNestedDailyAggregation(
                reportDate,
                timezone,
                productsCollection,
                productBarcodeField,
                productClientEmailPath,
                requiredOrderStatus
        );

        AggregationResults<ClientAgg> results =
                mongoOperations.aggregate(aggregation, ordersCollection, ClientAgg.class);

        DayToDaySalesReportPojo reportDocument = new DayToDaySalesReportPojo();
        reportDocument.setId(reportDate.toString());
        reportDocument.setDate(reportDate);
        reportDocument.setClients(mapClientBlocks(results.getMappedResults()));

        return reportDocument;
    }

    private static Aggregation buildNestedDailyAggregation(
            LocalDate reportDate,
            ZoneId timezone,
            String productsCollection,
            String productBarcodeField,
            String productClientEmailPath,
            OrderStatus requiredOrderStatus
    ) {
        ZonedDateTime start = reportDate.atStartOfDay(timezone);
        ZonedDateTime endExclusive = reportDate.plusDays(1).atStartOfDay(timezone);

        MatchOperation matchInvoicedOrders = match(new Criteria().andOperator(
                Criteria.where("orderTime").gte(start).lt(endExclusive),
                Criteria.where("status").is(requiredOrderStatus.name())
        ));

        UnwindOperation unwindItems = unwind("orderItems");

        LookupOperation lookupProduct = LookupOperation.newLookup()
                .from(productsCollection)
                .localField("orderItems.productBarcode")
                .foreignField(productBarcodeField)
                .as("product");

        UnwindOperation unwindProduct = unwind("product");

        AggregationExpression lineRevenue =
                ArithmeticOperators.Multiply.valueOf("orderItems.orderedQuantity")
                        .multiplyBy("orderItems.sellingPrice");

        GroupOperation groupByClientAndProduct = group(
                Fields.from(
                        Fields.field("clientEmail", productClientEmailPath),
                        Fields.field("productBarcode", "orderItems.productBarcode")
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
                lookupProduct,
                unwindProduct,
                groupByClientAndProduct,
                projectClientProductRow,
                groupProductsUnderClient,
                projectClientBlock,
                sortClientsByRevenue
        );
    }

    private static List<DayToDaySalesReportPojo.ClientBlock> mapClientBlocks(List<ClientAgg> clientAggs) {
        List<DayToDaySalesReportPojo.ClientBlock> clients = new ArrayList<>();
        if (clientAggs == null) return clients;

        for (ClientAgg clientAgg : clientAggs) {
            DayToDaySalesReportPojo.ClientBlock client = new DayToDaySalesReportPojo.ClientBlock();
            client.setClientEmail(clientAgg.getClientEmail());
            client.setOrdersCount(nullSafeLong(clientAgg.getOrdersCount()));
            client.setItemsCount(nullSafeLong(clientAgg.getItemsCount()));
            client.setTotalRevenue(nullSafeDouble(clientAgg.getTotalRevenue()));
            client.setProducts(mapProductBlocks(clientAgg.getProducts()));
            clients.add(client);
        }

        return clients;
    }

    private static List<DayToDaySalesReportPojo.ProductBlock> mapProductBlocks(List<ProductAgg> productAggs) {
        List<DayToDaySalesReportPojo.ProductBlock> products = new ArrayList<>();
        if (productAggs == null) return products;

        for (ProductAgg productAgg : productAggs) {
            DayToDaySalesReportPojo.ProductBlock product = new DayToDaySalesReportPojo.ProductBlock();
            product.setProductBarcode(productAgg.getProductBarcode());
            product.setOrdersCount(nullSafeLong(productAgg.getOrdersCount()));
            product.setItemsCount(nullSafeLong(productAgg.getItemsCount()));
            product.setTotalRevenue(nullSafeDouble(productAgg.getTotalRevenue()));
            products.add(product);
        }

        return products;
    }

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

    private static long nullSafeLong(Long v) { return v == null ? 0L : v; }
    private static double nullSafeDouble(Double v) { return v == null ? 0.0 : v; }
}
