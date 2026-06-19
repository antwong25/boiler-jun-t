package org.example.boilerserver.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.boilerserver.config.JwtProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private static final String CLAIM_USER_TYPE = "userType";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        if (!StringUtils.hasText(jwtProperties.getSecret()) || jwtProperties.getSecret().trim().length() < 32) {
            throw new IllegalStateException("JWT secret 长度至少需要 32 个字符");
        }
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String userId, String userType) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.getExpirationMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(userId)
                .issuer(jwtProperties.getIssuer())
                .claim(CLAIM_USER_TYPE, userType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public AuthUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String userId = claims.getSubject();
            String userType = claims.get(CLAIM_USER_TYPE, String.class);
            if (!StringUtils.hasText(userId) || !StringUtils.hasText(userType)) {
                throw new IllegalArgumentException("JWT 缺少必要用户信息");
            }
            return new AuthUser(userId, userType);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("无效或已过期的登录凭证");
        }
    }

    public long getExpirationMinutes() {
        return jwtProperties.getExpirationMinutes();
    }
}
