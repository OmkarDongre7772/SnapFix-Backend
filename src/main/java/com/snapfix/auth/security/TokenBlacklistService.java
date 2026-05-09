package com.snapfix.auth.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {
    
    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

    public void blacklist(String token, Instant expiry){
        blacklist.put(token, expiry);
    }

    public boolean isBlacklisted(String token){
        Instant expiry = blacklist.get(token);
        if(expiry == null)return false; // not an blacklisted token 
        if(expiry.isBefore(Instant.now())){
            blacklist.remove(token);    // token is blacklisted and naturally expired, remove it
            return false;
        }
        return true;
    }

    // clear the expired tokens
    public void clearExpired(){
        Instant now  = Instant.now();
        blacklist.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
