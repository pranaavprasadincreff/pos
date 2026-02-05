package com.increff.pos.api;

import com.increff.pos.dao.SalesReportDao;
import com.increff.pos.db.SalesReportRowPojo;
import com.increff.pos.model.constants.ReportRowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SalesReportApiImpl implements SalesReportApi {
    @Autowired
    private SalesReportDao salesReportDao;

    @Override
    public List<SalesReportRowPojo> getDailyReport(LocalDate reportDate, String clientEmail, ReportRowType rowType) {
        return salesReportDao.getDailyReportRows(reportDate, clientEmail, rowType);
    }

    @Override
    public List<SalesReportRowPojo> getRangeReport(LocalDate startDate, LocalDate endDate, String clientEmail, ReportRowType rowType) {
        return salesReportDao.getRangeReportRows(startDate, endDate, clientEmail, rowType);
    }

    @Override
    public void generateAndStoreDailyNested(LocalDate reportDate) {
        salesReportDao.generateAndStoreDailyReportDocument(reportDate);
    }

    @Override
    public boolean existsDailyNested(LocalDate reportDate) {
        return salesReportDao.existsDailyReportDocument(reportDate);
    }
}
