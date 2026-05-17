package com.snapfix.admin.service;

import com.snapfix.bid.entity.Bid;
import com.snapfix.bid.entity.BidStatus;
import com.snapfix.bid.repository.BidRepository;
import com.snapfix.bid.service.BidService;
import com.snapfix.user.entity.User;

import com.snapfix.user.service.UserService;

import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.snapfix.admin.entity.AdminActionLog;
import com.snapfix.admin.repository.AdminRepository;
import com.snapfix.auth.security.CustomUserDetails;

@Service
public class AdminService {
    private final BidService bidService;
    private final AdminRepository adminRepository;
    private final UserService userService;

    public AdminService(
        AdminRepository adminRepository, BidService bidService, UserService userService
    ){
        this.adminRepository = adminRepository;
        this.bidService = bidService;
        this.userService = userService;
    }

    @Transactional
    public void approveBid(UUID bidId) {
        Bid bid = bidService.approveBid(bidId);
        User admin = userService.getUserById(getCurrentUserId());
        log(admin, bid.getWorker().getId(), "Bid Approved By "+getCurrentUserId()+", for Worker "+bid.getWorker().getId(), "");
    }

    @Transactional
    public void rejectBid(UUID bidId) {
        Bid bid = bidService.rejectBid(bidId);
        User admin = userService.getUserById(getCurrentUserId());
        log(admin, bid.getWorker().getId(), "Bid Rejected By "+getCurrentUserId()+", for Worker "+bid.getWorker().getId(), "");
    }

/*
        UTILITY FUNCTIONS
*/

    public void log(User admin, UUID targetId, String action, String note){
        AdminActionLog log = new AdminActionLog();
        log.setAction(action);
        log.setAdmin(admin);
        log.setNote(note);
        log.setTargetId(targetId);
        log.setTimestamp(Instant.now());
        adminRepository.save(log);
    }
    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new IllegalStateException("Invalid authentication context");
        }

        return user.getId();
    }
    
}
