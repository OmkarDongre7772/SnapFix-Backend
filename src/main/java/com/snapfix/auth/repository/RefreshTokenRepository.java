package com.snapfix.auth.repository;

import com.snapfix.auth.entity.RefreshToken;
import com.snapfix.user.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUser(User user);
}