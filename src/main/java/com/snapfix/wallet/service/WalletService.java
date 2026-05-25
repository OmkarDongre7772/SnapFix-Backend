package com.snapfix.wallet.service;

import com.snapfix.user.entity.User;
import com.snapfix.wallet.repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.snapfix.auth.security.CustomUserDetails;
import com.snapfix.wallet.dto.WalletResponse;
import com.snapfix.wallet.entity.Transaction;
import com.snapfix.wallet.entity.Wallet;
import com.snapfix.wallet.repository.WalletRepository;

@Service
public class WalletService {
    
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository, TransactionRepository transactionRepository){
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    public Wallet getWallet(UUID id) {
        return walletRepository.findByWorker_Id(id);
    }

    public void saveWallet(Wallet wallet) {
        walletRepository.save(wallet);
    }

    public void saveTransaction(Transaction transaction) {
        transactionRepository.save(transaction);
    }

    @PreAuthorize("hasRole('WORKER')")
    public WalletResponse getWorkerWallet(){
        return WalletResponse.mapToResponse(walletRepository.findByWorker_Id(getCurrentUserId()));
    }

    public Wallet createWallet(User user){
        Wallet wallet = new Wallet();
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setWorker(user);
        return wallet;
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new IllegalStateException("Invalid authentication context");
        }

        return user.getId();
    }
}
