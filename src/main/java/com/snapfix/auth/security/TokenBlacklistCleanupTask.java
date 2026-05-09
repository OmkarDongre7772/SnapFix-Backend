package com.snapfix.auth.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TokenBlacklistCleanupTask {

    private final TokenBlacklistService tokenBlacklistService;

    public TokenBlacklistCleanupTask(TokenBlacklistService tokenBlacklistService){
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Scheduled(fixedRate = 900000)
    public void clearExpiredTokens(){
        tokenBlacklistService.clearExpired();
    }
    
}
