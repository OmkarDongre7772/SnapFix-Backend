package com.snapfix.payment.service;

import com.snapfix.bid.service.BidService;
import com.snapfix.notification.entity.NotificationType;
import com.snapfix.notification.service.NotificationService;
import com.snapfix.report.service.ReportService;
import com.snapfix.wallet.entity.Transaction;
import com.snapfix.wallet.entity.Wallet;
import com.snapfix.wallet.service.WalletService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.snapfix.auth.security.CustomUserDetails;
import com.snapfix.payment.dto.PaymentResponse;
import com.snapfix.payment.entity.Payment;
import com.snapfix.payment.entity.PaymentStatus;
import com.snapfix.payment.repository.PaymentRepository;
import com.snapfix.report.entity.ReportStatus;
import com.snapfix.task.entity.Task;
import com.snapfix.task.entity.TaskStatus;
import com.snapfix.task.service.TaskService;

import jakarta.transaction.Transactional;

@Service
public class PaymentService {
    private final NotificationService notificationService;
    private final ReportService reportService;
    private final BidService bidService;
    private final TaskService taskService;
    private final WalletService walletService;
    private final PaymentRepository paymentRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            TaskService taskService,
            WalletService walletService,
            BidService bidService,
            ReportService reportService,
            NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.taskService = taskService;
        this.walletService = walletService;
        this.bidService = bidService;
        this.reportService = reportService;
        this.notificationService = notificationService;
    }

    public List<PaymentResponse> getPaymentHistory() {
        return paymentRepository.findAllByWorker_Id(getCurrentUserId())
                .stream()
                .sorted(Comparator.comparing(
                        Payment::getReleasedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(PaymentResponse::mapToResponse)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse releasePayment(UUID taskId) {

        Payment payment = paymentRepository.findByTask_Id(taskId);
        if (payment == null) {
            throw new IllegalStateException("Pending Payment not Found.");
        } else if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("The payment cannot be released.");
        }
        Task task = taskService.getTaskById(taskId);
        Wallet wallet = walletService.getWallet(task.getWorker().getId());
        if (task.getStatus() != TaskStatus.COMPLETED) {
            throw new IllegalStateException("Connot release payment for incomplete task.");
        } else if (wallet == null) {
            throw new IllegalStateException("Worker's wallet do not exist.");
        }
        BigDecimal amount = bidService.getApprovedBidForReport(task.getReport().getId()).getBidAmount();
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Bid Amount should not be negative");
        }
        wallet.setBalance(wallet.getBalance().add(amount));
        task.setStatus(TaskStatus.PAYMENT_RELEASED);
        task.getReport().setStatus(ReportStatus.COMPLETED);

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setReferenceId(payment);
        transaction.setTimestamp(Instant.now());
        transaction.setType("CREDIT");
        transaction.setWalletId(wallet);

        reportService.saveReport(task.getReport());
        taskService.saveTask(task);
        walletService.saveWallet(wallet);
        payment.setStatus(PaymentStatus.RELEASED);
        payment.setReleasedAt(Instant.now());
        paymentRepository.save(payment);
        walletService.saveTransaction(transaction);
        notificationService.createNotification(task.getWorker(), NotificationType.PAYMENT_RELEASED,
                "Payment " + amount.toPlainString() + " Released for Task " + taskId);
        return PaymentResponse.mapToResponse(payment);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new IllegalStateException("Invalid authentication context");
        }

        return user.getId();
    }

}
