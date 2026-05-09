package com.snapfix.worker.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snapfix.worker.service.WorkerService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.snapfix.common.entity.Location;
import com.snapfix.report.dto.ReportResponse;


@RestController
@RequestMapping("/workers")
public class WorkerController {

    WorkerService workerService;

    public WorkerController(WorkerService workerService){
        this.workerService = workerService;
    }
    
    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/reports/nearby")
    public ResponseEntity<List<ReportResponse>> getNearByReports(@RequestParam Double lat, @RequestParam Double lng) {
        return ResponseEntity.ok(workerService.getNearbyReports(new Location(lat, lng)));
    }
    
}
