package com.snapfix.admin.controller;

import com.snapfix.admin.service.AdminService;
import com.snapfix.bid.service.BidService;
import com.snapfix.report.service.ReportService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snapfix.bid.dto.BidResponseDTO;
import com.snapfix.report.dto.ReportResponse;
import com.snapfix.report.entity.ReportStatus;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;



@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final AdminService adminService;
    private final BidService bidService;
    private final ReportService reportService;

    AdminController(ReportService reportService, BidService bidService, AdminService adminService) {
        this.reportService = reportService;
        this.bidService = bidService;
        this.adminService = adminService;
    }

    @GetMapping("/reports")
    public ResponseEntity<Page<ReportResponse>> getAllReports(
        @RequestParam(required = false) ReportStatus status,
        Pageable pageable
    ) {
        return ResponseEntity.ok(reportService.getAllReports(status, pageable));
    }

    @GetMapping("/reports/{id}/bids")
    public ResponseEntity<List<BidResponseDTO>> getBidsForReport(@PathVariable UUID id) {
        return ResponseEntity.ok(bidService.viewBidsForReportId(id));
    }
    
    @PostMapping("/bids/{bidId}/approve")
    public ResponseEntity<Void> approveBid(@PathVariable UUID bidId) {
        adminService.approveBid(bidId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/bids/{bidId}/reject")
    public ResponseEntity<Void> rejectBid(@PathVariable UUID bidId) {
        adminService.rejectBid(bidId);
        return ResponseEntity.ok().build();
    }
}
