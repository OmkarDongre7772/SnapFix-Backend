package com.snapfix.report.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.snapfix.report.dto.ReportRequest;
import com.snapfix.report.dto.ReportResponse;
import com.snapfix.report.entity.Category;
import com.snapfix.report.service.ReportService;



@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    //Create Report
    @PreAuthorize("hasRole('CITIZEN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportResponse> createReport(
            @RequestPart("image") MultipartFile image,
            @RequestParam String description,
            @RequestParam Category category,
            @RequestParam Double lat,
            @RequestParam Double lng) {

        ReportRequest request = new ReportRequest();
        request.setDescription(description);
        request.setCategory(category);
        request.setLat(lat);
        request.setLng(lng);

        return ResponseEntity.ok(reportService.createReport(request, image));
    }

    //Get Report by ID
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<ReportResponse> getReport(@PathVariable UUID id) {
        return ResponseEntity.ok(reportService.getReportById(id));
    }

    //Support a Report
    @PreAuthorize("hasRole('CITIZEN')")
    @PostMapping("/{id}/support")
    public ResponseEntity<ReportResponse> supportReport(@PathVariable UUID id) {
        System.out.println("Entered into Support");
        return ResponseEntity.ok(reportService.supportReport(id));
    }

    //Nearby Reports
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/nearby")
    public ResponseEntity<List<ReportResponse>> getNearbyReports(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5000") double radius) {

        return ResponseEntity.ok(reportService.getNearbyReports(lat, lng, radius));
    }



}
