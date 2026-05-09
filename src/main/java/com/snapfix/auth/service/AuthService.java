package com.snapfix.auth.service;

import java.time.Instant;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.snapfix.auth.dto.AuthResponse;
import com.snapfix.auth.dto.RegisterRequest;
import com.snapfix.auth.entity.RefreshToken;
import com.snapfix.auth.repository.RefreshTokenRepository;
import com.snapfix.auth.security.TokenBlacklistService;
import com.snapfix.common.util.JwtUtil;
import com.snapfix.user.entity.CitizenProfile;
import com.snapfix.user.entity.Role;
import com.snapfix.user.entity.User;
import com.snapfix.user.entity.WorkerProfile;
import com.snapfix.user.repository.CitizenProfileRepository;
import com.snapfix.user.repository.UserRepository;
import com.snapfix.user.repository.WorkerProfileRepository;

import jakarta.transaction.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final CitizenProfileRepository citizenRepo;
    private final WorkerProfileRepository workerRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    private static final long REFRESH_TOKEN_EXPIRY = 7 * 24 * 60 * 60;
    private final TokenBlacklistService tokenBlacklistService;

    // Constructor
    public AuthService(
            UserRepository userRepository,
            CitizenProfileRepository citizenProfileRepository,
            WorkerProfileRepository workerProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            RefreshTokenRepository refreshTokenRepository,
            TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.citizenRepo = citizenProfileRepository;
        this.workerRepo = workerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    // Registeration
    @Transactional
    public void register(RegisterRequest request) {

        // Check if email exists
        userRepository.findByEmail(request.getEmail()).ifPresent(u -> {
            throw new IllegalStateException("Email already exists");
        });
        if (request.getRole() == null) {
            throw new IllegalArgumentException("Role is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }

        // Create User
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user = userRepository.save(user);
        // User Created

        // Profile Based on Role Creation
        if (request.getRole() == Role.CITIZEN) {
            // Citizen Creation
            CitizenProfile profile = new CitizenProfile();
            profile.setUser(user);
            profile.setName(request.getName());
            citizenRepo.save(profile);
        } else if (request.getRole() == Role.WORKER) {
            // Worker Creation
            WorkerProfile profile = new WorkerProfile();
            profile.setUser(user);
            profile.setName(request.getName());
            workerRepo.save(profile);
        }

    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalStateException("Invalid refresh token"));

        if (token.isRevoked()) {
            throw new IllegalStateException("Token revoked");
        }

        if (token.getExpiryDate().isBefore(Instant.now())) {
            throw new IllegalStateException("Token expired");
        }

        // Generate new access token
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        String newAccessToken = jwtUtil.generateToken(token.getUser().getEmail());
        String newRefreshToken = jwtUtil.generateRefreshToken(token.getUser().getEmail());

        RefreshToken newToken = new RefreshToken();
        newToken.setToken(newRefreshToken);
        newToken.setUser(token.getUser());
        newToken.setExpiryDate(Instant.now().plusSeconds(7 * 24 * 60 * 60));
        newToken.setRevoked(false);

        refreshTokenRepository.save(newToken);

        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public AuthResponse login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String accessToken = jwtUtil.generateToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // Revoke all existing tokens and SAVE them
        List<RefreshToken> existingTokens = refreshTokenRepository.findByUser(user);
        existingTokens.forEach(t -> t.setRevoked(true));
        refreshTokenRepository.saveAll(existingTokens); // ← was missing before

        // Save new refresh token
        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setToken(refreshToken);
        tokenEntity.setUser(user);
        tokenEntity.setExpiryDate(Instant.now().plusSeconds(REFRESH_TOKEN_EXPIRY));
        tokenEntity.setRevoked(false);
        refreshTokenRepository.save(tokenEntity);

        return new AuthResponse(accessToken, refreshToken);
    }

    @Transactional
    public void logout(String refreshToken, String accessToken) {
        // Revoking refresh token
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (token.isRevoked()) {
            throw new IllegalArgumentException("Token already revoked");
        }

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        // Blacklisting access token till active
        if(accessToken != null && !accessToken.isBlank()){
            try{
                Instant expiry = jwtUtil.getExpiry(accessToken);
                tokenBlacklistService.blacklist(accessToken, expiry);
            }catch( Exception e){
                
            }
        }
    }
}
