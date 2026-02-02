package com.increff.pos.api;

import com.increff.pos.dao.SalesReportDao;
import com.increff.pos.db.SalesReportAggregatePojo;
import com.increff.pos.db.SalesReportRowPojo;
import com.increff.pos.model.constants.ReportRowType;
import com.increff.pos.model.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.List;

@Service
public class SalesReportApiImpl implements SalesReportApi {

    @Autowired
    private SalesReportDao salesReportDao;

    @Override
    public List<SalesReportRowPojo> getDailyReport(LocalDate date, String clientEmail, ReportRowType type) throws ApiException {
        List<SalesReportRowPojo> stored = salesReportDao.fetchDaily(date, clientEmail, type);
        if (!CollectionUtils.isEmpty(stored)) return stored;
        return salesReportDao.computeFromOrdersForSingleDay(date, clientEmail, type);
    }

    @Override
    public List<SalesReportRowPojo> getRangeReport(LocalDate start, LocalDate end, String clientEmail, ReportRowType type) throws ApiException {
        return salesReportDao.computeFromOrdersForRange(start, end, clientEmail, type);
    }

    @Override
    public void generateAndStoreDaily(LocalDate date) throws ApiException {
        List<SalesReportAggregatePojo> docs = salesReportDao.buildDailyAggregatesFacet(date);
        salesReportDao.replaceDailyAggregates(date, docs);
    }
}
