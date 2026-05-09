package com.snapfix.worker.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.snapfix.common.entity.Location;
import com.snapfix.report.dto.ReportResponse;
import com.snapfix.report.service.ReportService;

@Service
public class WorkerService {

    ReportService reportService;

    public WorkerService(ReportService reportService){
        this.reportService = reportService;
    }

    public List<ReportResponse> getNearbyReports(Location location) {
        return reportService.getNearbyReports(location.getLatitude(), location.getLongitude(), 5000);
    }
    
}
