package com.snapfix.bid.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snapfix.bid.dto.BidRequestDTO;
import com.snapfix.bid.dto.BidResponseDTO;
import com.snapfix.bid.service.BidService;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/bids")
public class BidController {

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<BidResponseDTO> createBid(@RequestBody BidRequestDTO request) {

        return ResponseEntity.ok(bidService.createBid(request));
    }

    @DeleteMapping("/{bidId}")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<String> withdrawBid(@PathVariable UUID bidId) {

        bidService.withdrawBid(bidId);
        return ResponseEntity.ok("Bid deleted successfully");

    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('WORKER')")
    public List<BidResponseDTO> getWorkerBids() {
        return bidService.viewBidsFromWorker();
    }

}
