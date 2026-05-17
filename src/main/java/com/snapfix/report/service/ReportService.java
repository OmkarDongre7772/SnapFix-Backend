package com.snapfix.report.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Point;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.snapfix.auth.security.CustomUserDetails;
import com.snapfix.geo.util.GeoUtil;
import com.snapfix.notification.entity.NotificationType;
import com.snapfix.notification.service.NotificationService;
import com.snapfix.report.dto.ReportRequest;
import com.snapfix.report.dto.ReportResponse;
import com.snapfix.report.entity.Report;
import com.snapfix.report.entity.ReportStatus;
import com.snapfix.report.entity.ReportSupport;
import com.snapfix.report.repository.ReportRepository;
import com.snapfix.report.repository.ReportSupportRepository;
import com.snapfix.storage.service.StorageService;
import com.snapfix.user.service.UserService;

import jakarta.transaction.Transactional;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportSupportRepository reportSupportRepository;
    private final StorageService storageService;
    private final UserService userService;
    private final NotificationService notificationService;

    public ReportService(ReportRepository reportRepository,
            StorageService storageService,
            ReportSupportRepository reportSupportRepository,
            UserService userService,
            NotificationService notificationService) {
        this.reportRepository = reportRepository;
        this.storageService = storageService;
        this.reportSupportRepository = reportSupportRepository;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    // Create Report

    @Transactional
    @PreAuthorize("hasRole('CITIZEN')")
    public ReportResponse createReport(ReportRequest request, MultipartFile image) {
        UUID citizenId = getCurrentUserId();
        validateCreateRequest(request, image);

        List<Report> nearby = reportRepository.findNearbyReports(
                request.getLat(), request.getLng(), 50);

        Report existing = nearby.stream()
                .filter(report -> report.getCategory() == request.getCategory())
                .findFirst()
                .orElse(null);

        if (existing != null) {
            addSupport(existing, citizenId, "You already reported this issue");
            Report saved = reportRepository.save(existing);
            notificationService.createNotification(
                    userService.getUserById(existing.getCitizenId()),
                    NotificationType.REPORT_SUPPORTED,
                    "Someone supported your " + existing.getCategory() + " report");
            return mapToResponse(saved, "Existing report found - your support has been added");
        }

        String imageUrl = storageService.uploadImage(image);

        Report report = new Report();
        report.setCitizenId(citizenId);
        report.setCategory(request.getCategory());
        report.setDescription(request.getDescription());
        report.setImageUrl(imageUrl);
        report.setLocation(GeoUtil.createPoint(request.getLat(), request.getLng()));
        report.setCreatedAt(Instant.now());
        report.setStatus(ReportStatus.CREATED);
        report.setSupportCount(1);

        // Saving the Report
        Report saved = reportRepository.save(report);

        // Incrementing the Submitted Reports of the Citizen
        userService.incrementReportSubmitted(citizenId);

        // Adding the support by the citizen to the report in Support's Table
        createSupport(saved.getId(), citizenId, "Already supported");
        notificationService.createNotification(
                userService.getUserById(citizenId),
                NotificationType.REPORT_CREATED,
                "Your " + saved.getCategory() + " report has been created successfully");

        return mapToResponse(saved, "Report created successfully");
    }

    public ReportResponse getReportById(UUID id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        return mapToResponse(report);
    }

    public Report getReport(UUID id){
        return reportRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Report not found"));
    }

    @Transactional
    @PreAuthorize("hasRole('CITIZEN')")
    public ReportResponse supportReport(UUID id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        addSupport(report, getCurrentUserId(), "Already supported");
        Report savedReport = reportRepository.save(report);
        notificationService.createNotification(
                userService.getUserById(savedReport.getCitizenId()),
                NotificationType.REPORT_SUPPORTED,
                "Someone supported your " + savedReport.getCategory() + " report");

        return mapToResponse(savedReport, "Support added successfully");
    }

    @PreAuthorize("isAuthenticated()")
    public List<ReportResponse> getNearbyReports(double lat, double lng, double radius) {
        validateLocation(lat, lng);
        return reportRepository.findNearbyReports(lat, lng, radius).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void addSupport(Report report, UUID userId, String duplicateMessage) {
        if (reportSupportRepository.existsByReportIdAndUserId(report.getId(), userId)) {
            throw new IllegalArgumentException(duplicateMessage);
        }

        createSupport(report.getId(), userId, duplicateMessage);
        report.setSupportCount(report.getSupportCount() + 1);
    }

    private void createSupport(UUID reportId, UUID userId, String duplicateMessage) {
        ReportSupport support = new ReportSupport();
        support.setReportId(reportId);
        support.setUserId(userId);
        support.setCreatedAt(Instant.now());

        try {
            reportSupportRepository.save(support);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException(duplicateMessage);
        }
    }

    // ADMIN METHOD
    public Page<ReportResponse> getAllReports(ReportStatus status, Pageable pageable) {
        Page<Report> reports;
        if (status != null) {
            reports = reportRepository.findByStatus(status, pageable);
        } else {
            reports = reportRepository.findAll(pageable);
        }
        return reports.map(ReportResponse::new);
    }

    /*
     * UTILITY FUNCTIONS
     */

    private void validateCreateRequest(ReportRequest request, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image is required");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (request.getDescription().length() > 1000) {
            throw new IllegalArgumentException("Description must be at most 1000 characters");
        }
        if (request.getCategory() == null) {
            throw new IllegalArgumentException("Category is required");
        }
        if (request.getLat() == null || request.getLat() < -90 || request.getLat() > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (request.getLng() == null || request.getLng() < -180 || request.getLng() > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    private ReportResponse mapToResponse(Report report) {
        return mapToResponse(report, null);
    }

    private ReportResponse mapToResponse(Report report, String message) {
        ReportResponse response = new ReportResponse();

        response.setId(report.getId());
        response.setCitizenId(report.getCitizenId());
        response.setImageUrl(report.getImageUrl());
        response.setDescription(report.getDescription());
        response.setCategory(report.getCategory());

        Point point = report.getLocation();
        response.setLat(point.getY());
        response.setLng(point.getX());

        response.setStatus(report.getStatus());
        response.setSupportCount(report.getSupportCount());
        response.setCreatedAt(report.getCreatedAt());
        response.setMessage(message);

        return response;
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new IllegalStateException("Invalid authentication context");
        }

        return user.getId();
    }

    public void saveReport(Report report){
        reportRepository.save(report);
    }

    private void validateLocation(double lat, double lng){
        if ( lat  < -90 || lat  > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (lng < -180 || lng > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }
}
