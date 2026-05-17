package com.mianshiba.ai.utils;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtils {

    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String USER_ACCOUNT_CLAIM = "userAccount";
    private static final String USER_ROLE_CLAIM = "userRole";

    private final SecretKey secretKey;
    private final Duration expiration;

    public JwtUtils(@Value("${spring.security.jwt.secret}") String secret,
                    @Value("${spring.security.jwt.expiration:PT24H}") Duration expiration) {
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(Long userId, String userAccount, String userRole) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(USER_ACCOUNT_CLAIM, userAccount)
                .claim(USER_ROLE_CLAIM, userRole)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public JwtUserClaims parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new JwtUserClaims(
                    Long.valueOf(claims.getSubject()),
                    claims.get(USER_ACCOUNT_CLAIM, String.class),
                    claims.get(USER_ROLE_CLAIM, String.class)
            );
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
    }

    public String resolveToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(TOKEN_PREFIX)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        String token = authorizationHeader.substring(TOKEN_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return token;
    }

    public record JwtUserClaims(Long userId, String userAccount, String userRole) {
    }
}
