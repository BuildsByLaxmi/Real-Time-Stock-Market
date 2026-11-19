package com.example.stockmarket.auth.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByJti(String jti);

    @Modifying
    @Query("delete from RevokedToken r where r.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
