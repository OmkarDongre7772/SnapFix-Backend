package com.snapfix.bid.service;

import com.snapfix.task.service.TaskService;
import com.snapfix.user.entity.User;
import com.snapfix.user.service.UserService;

import com.snapfix.worker.service.WorkerService;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.snapfix.auth.security.CustomUserDetails;
import com.snapfix.bid.dto.BidRequestDTO;
import com.snapfix.bid.dto.BidResponseDTO;
import com.snapfix.bid.entity.Bid;
import com.snapfix.bid.entity.BidStatus;
import com.snapfix.bid.repository.BidRepository;
import com.snapfix.report.entity.Report;
import com.snapfix.report.entity.ReportStatus;
import com.snapfix.report.service.ReportService;
import com.snapfix.task.entity.Task;
import com.snapfix.task.entity.TaskStatus;

@Service
public class BidService {

    /*
     * DECLARATIONS
     */

    private final WorkerService workerService;
    private final TaskService taskService;
    private final UserService userService;
    private final BidRepository bidRepository;
    private final ReportService reportService;

    BidService(BidRepository bidRepository, UserService userService, ReportService reportService, TaskService taskService, WorkerService workerService) {
        this.bidRepository = bidRepository;
        this.userService = userService;
        this.reportService = reportService;
        this.taskService = taskService;
        this.workerService = workerService;
    }

    /*
     * CORE FUNCTIONS
     */

    @Transactional
    public BidResponseDTO createBid(BidRequestDTO request) {
        if(workerService.getWorkerProfile() == null){
            throw new IllegalStateException("Cannot create Bid for Worker without Worker Profile");
        }
        if(!workerService.getWorkerProfile().isAvailable()){
            throw new IllegalStateException("Inactive worker cannot Bid, update profile status to Active before Bidding");
        }
        validateCreateRequest(request);

        Report report = reportService.getReport(
                request.getReportId());

        if (report.getStatus() != ReportStatus.CREATED) {
            throw new IllegalStateException(
                    "Cannot bid on non-active report");
        }

        UUID workerId = getCurrentUserId();

        if (bidRepository.existsByReport_IdAndWorker_Id(
                report.getId(),
                workerId)) {
            throw new IllegalStateException(
                    "Duplicate bid not allowed");
        }

        User worker = userService.getUserById(workerId);

        Bid bid = new Bid(
                report,
                worker,
                request.getBidAmount(),
                request.getDurationEstimate(),
                request.getResourceNote() == null ? "" : request.getResourceNote());

        bidRepository.save(bid);

        return new BidResponseDTO(bid);
    }

    @Transactional
    public void withdrawBid(UUID bidId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new IllegalArgumentException("Bid not found or already deleted"));

        if (!bid.getWorker().getId().equals(getCurrentUserId()))
            throw new AccessDeniedException("Invalid Access, Access Denied!");
        if (bid.getStatus() != BidStatus.ACTIVE)
            throw new IllegalStateException("Cannot withdraw Non-Active bids");
        bid.setStatus(BidStatus.WITHDRAWN);
        bidRepository.save(bid);

    }

    public List<BidResponseDTO> viewBidsFromWorker() {
        List<BidResponseDTO> response = bidRepository.findByWorker_Id(getCurrentUserId()).stream()
                .map(bid -> new BidResponseDTO(bid)).toList();
        return response;
    }

    public List<BidResponseDTO> viewBidsForReportId(UUID id) {
        List<BidResponseDTO> response = bidRepository.findByReport_Id(id).stream().map(bid -> new BidResponseDTO(bid))
                .toList();
        return response;
    }

    @Transactional
    public Bid approveBid(UUID bidId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new IllegalArgumentException("Bid not found"));

        if(bid.getStatus() != BidStatus.ACTIVE){
            throw new IllegalStateException("Cannot approve non-active or approved bid");
        }
        if (bid.getReport().getStatus() != ReportStatus.CREATED) {
            throw new IllegalStateException("Cannot approve a bid for a report that is already assigned");
        }

        List<Bid> list = bidRepository.findByReport_Id(bid.getReport().getId());
        for (Bid b : list) {
            if (b.getId().equals(bid.getId())) {
                continue;
            }
            b.setStatus(BidStatus.REJECTED);
            bidRepository.save(b);
        }
        bid.setStatus(BidStatus.APPROVED);
        bidRepository.save(bid);

        Task task = new Task();
        task.setReport(bid.getReport());
        task.setWorker(bid.getWorker());
        task.setStatus(TaskStatus.ASSIGNED);
        taskService.saveTask(task);
        bid.getReport().setStatus(ReportStatus.IN_PROGRESS);
        reportService.saveReport(bid.getReport());
        return bid;
    }

    @Transactional
    public Bid rejectBid(UUID bidId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new IllegalArgumentException("Bid not found"));
        if (bid.getStatus() != BidStatus.ACTIVE) {
            throw new IllegalStateException("Cannot reject non-active bid");
        }
        bid.setStatus(BidStatus.REJECTED);
        return bidRepository.save(bid);
    }

    /*
     * UTILITY FUNCTIONS
     */

    void validateCreateRequest(BidRequestDTO request) {
        if (request.getReportId() == null) {
            throw new IllegalArgumentException("Report Id cannot be null, provide valid Report Id");
        }
        if (request.getBidAmount() == null || request.getBidAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Bid Amount cannot be null or negative");
        }
        if (request.getDurationEstimate() < 0) {
            throw new IllegalArgumentException("Duration Estimate cannot be null or negative");
        }
        if (request.getResourceNote() != null && request.getResourceNote().trim().length() > 5000) {
            throw new IllegalArgumentException("Resource Note cannot be over 5000 alphabets");
        }
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new IllegalStateException("Invalid authentication context");
        }

        return user.getId();
    }
}
