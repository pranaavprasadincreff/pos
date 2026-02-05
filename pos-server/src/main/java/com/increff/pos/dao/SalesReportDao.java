package com.increff.pos.dao;

import com.increff.pos.db.DayToDaySalesReportPojo;
import com.increff.pos.db.SalesReportRowPojo;
import com.increff.pos.helper.SalesReportAggregationHelper;
import com.increff.pos.helper.SalesReportRangeAccumulator;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.constants.ReportRowType;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

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
        if (reportDate == null) {
            return false;
        }
        Query query = Query.query(Criteria.where("_id").is(getDailyDocumentId(reportDate)));
        return mongoOperations.exists(query, DayToDaySalesReportPojo.class, DAILY_REPORT_COLLECTION);
    }

    public void generateAndStoreDailyReportDocument(LocalDate reportDate) {
        if (reportDate == null) {
            return;
        }
        DayToDaySalesReportPojo reportDocument = buildDailyReportDocumentFromOrders(reportDate);
        saveDailyReportDocument(reportDocument);
    }

    public List<SalesReportRowPojo> getDailyReportRows(LocalDate reportDate, String clientEmail, ReportRowType rowType) {
        if (reportDate == null) {
            return List.of();
        }
        DayToDaySalesReportPojo reportDocument = fetchOrCreateDailyReportDocument(reportDate);
        return mapDailyDocumentToRows(reportDocument, clientEmail, rowType);
    }

    public List<SalesReportRowPojo> getRangeReportRows(
            LocalDate startDate,
            LocalDate endDate,
            String clientEmail,
            ReportRowType rowType
    ) {
        if (startDate == null || endDate == null) {
            return List.of();
        }

        Map<LocalDate, DayToDaySalesReportPojo> existingDocumentsByDate =
                fetchDailyReportDocumentsInRange(startDate, endDate);

        List<DayToDaySalesReportPojo> completeDocuments =
                fetchOrGenerateMissingDocuments(startDate, endDate, existingDocumentsByDate);

        if (rowType == ReportRowType.CLIENT) {
            return SalesReportRangeAccumulator.accumulateClientRows(completeDocuments);
        }

        return SalesReportRangeAccumulator.accumulateProductRows(completeDocuments, clientEmail);
    }

    // -------------------- Fetch / Create strategy --------------------

    private DayToDaySalesReportPojo fetchOrCreateDailyReportDocument(LocalDate reportDate) {
        DayToDaySalesReportPojo existingDocument = fetchDailyReportDocument(reportDate);
        if (existingDocument != null) {
            return existingDocument;
        }

        DayToDaySalesReportPojo generatedDocument = buildDailyReportDocumentFromOrders(reportDate);
        saveDailyReportDocument(generatedDocument);

        return generatedDocument;
    }

    private DayToDaySalesReportPojo fetchDailyReportDocument(LocalDate reportDate) {
        Query query = Query.query(Criteria.where("_id").is(getDailyDocumentId(reportDate)));
        return mongoOperations.findOne(query, DayToDaySalesReportPojo.class, DAILY_REPORT_COLLECTION);
    }

    private void saveDailyReportDocument(DayToDaySalesReportPojo reportDocument) {
        if (reportDocument == null) {
            return;
        }
        mongoOperations.save(reportDocument, DAILY_REPORT_COLLECTION);
    }

    private Map<LocalDate, DayToDaySalesReportPojo> fetchDailyReportDocumentsInRange(LocalDate startDate, LocalDate endDate) {
        Query query = Query.query(Criteria.where("_id")
                .gte(getDailyDocumentId(startDate))
                .lte(getDailyDocumentId(endDate))
        );

        List<DayToDaySalesReportPojo> documents =
                mongoOperations.find(query, DayToDaySalesReportPojo.class, DAILY_REPORT_COLLECTION);

        Map<LocalDate, DayToDaySalesReportPojo> documentsByDate = new HashMap<>();
        if (documents == null) {
            return documentsByDate;
        }

        for (DayToDaySalesReportPojo document : documents) {
            if (document == null || document.getDate() == null) {
                continue;
            }
            documentsByDate.put(document.getDate(), document);
        }

        return documentsByDate;
    }

    private List<DayToDaySalesReportPojo> fetchOrGenerateMissingDocuments(
            LocalDate startDate,
            LocalDate endDate,
            Map<LocalDate, DayToDaySalesReportPojo> existingDocumentsByDate
    ) {
        List<DayToDaySalesReportPojo> completeDocuments = new ArrayList<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            DayToDaySalesReportPojo existingDocument = existingDocumentsByDate.get(currentDate);
            if (existingDocument != null) {
                completeDocuments.add(existingDocument);
                currentDate = currentDate.plusDays(1);
                continue;
            }

            DayToDaySalesReportPojo generatedDocument = buildDailyReportDocumentFromOrders(currentDate);
            saveDailyReportDocument(generatedDocument);
            completeDocuments.add(generatedDocument);

            currentDate = currentDate.plusDays(1);
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

    private String getDailyDocumentId(LocalDate reportDate) {
        return reportDate.toString();
    }

    // -------------------- Daily mapping (doc -> rows) --------------------

    private List<SalesReportRowPojo> mapDailyDocumentToRows(
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

    private List<SalesReportRowPojo> mapClientRows(List<DayToDaySalesReportPojo.ClientBlock> clientBlocks) {
        List<SalesReportRowPojo> rows = new ArrayList<>();
        if (clientBlocks == null) {
            return rows;
        }

        for (DayToDaySalesReportPojo.ClientBlock clientBlock : clientBlocks) {
            if (clientBlock == null) {
                continue;
            }

            SalesReportRowPojo row = new SalesReportRowPojo();
            row.setClientEmail(clientBlock.getClientEmail());
            row.setProductBarcode(null);
            row.setOrdersCount(nullSafeLong(clientBlock.getOrdersCount()));
            row.setItemsCount(nullSafeLong(clientBlock.getItemsCount()));
            row.setTotalRevenue(nullSafeDouble(clientBlock.getTotalRevenue()));
            rows.add(row);
        }

        rows.sort(Comparator.comparingDouble(SalesReportRowPojo::getTotalRevenue).reversed());
        return rows;
    }

    private List<SalesReportRowPojo> mapProductRowsForClient(
            List<DayToDaySalesReportPojo.ClientBlock> clientBlocks,
            String clientEmail
    ) {
        if (!StringUtils.hasText(clientEmail)) {
            return List.of();
        }

        DayToDaySalesReportPojo.ClientBlock clientBlock = findClientBlock(clientBlocks, clientEmail);
        if (clientBlock == null || clientBlock.getProducts() == null) {
            return List.of();
        }

        List<SalesReportRowPojo> rows = new ArrayList<>();
        for (DayToDaySalesReportPojo.ProductBlock productBlock : clientBlock.getProducts()) {
            if (productBlock == null) {
                continue;
            }

            SalesReportRowPojo row = new SalesReportRowPojo();
            row.setClientEmail(clientBlock.getClientEmail());
            row.setProductBarcode(productBlock.getProductBarcode());
            row.setOrdersCount(nullSafeLong(productBlock.getOrdersCount()));
            row.setItemsCount(nullSafeLong(productBlock.getItemsCount()));
            row.setTotalRevenue(nullSafeDouble(productBlock.getTotalRevenue()));
            rows.add(row);
        }

        rows.sort(Comparator.comparingDouble(SalesReportRowPojo::getTotalRevenue).reversed());
        return rows;
    }

    private DayToDaySalesReportPojo.ClientBlock findClientBlock(
            List<DayToDaySalesReportPojo.ClientBlock> clientBlocks,
            String clientEmail
    ) {
        if (clientBlocks == null || !StringUtils.hasText(clientEmail)) {
            return null;
        }

        for (DayToDaySalesReportPojo.ClientBlock clientBlock : clientBlocks) {
            if (clientBlock == null) {
                continue;
            }
            if (clientEmail.equals(clientBlock.getClientEmail())) {
                return clientBlock;
            }
        }

        return null;
    }

    private long nullSafeLong(Long value) {
        if (value == null) {
            return 0L;
        }
        return value;
    }

    private double nullSafeDouble(Double value) {
        if (value == null) {
            return 0.0;
        }
        return value;
    }
}
