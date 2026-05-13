package org.mj.trip.auth.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessTokenExpirationMsec;
    private final long refreshTokenExpirationMsec;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-expiration-msec}") long accessTokenExpirationMsec,
            @Value("${jwt.refresh-token-expiration-msec}") long refreshTokenExpirationMsec) {
        this.key = getKey(secretKey);
        this.accessTokenExpirationMsec = accessTokenExpirationMsec;
        this.refreshTokenExpirationMsec = refreshTokenExpirationMsec;
    }

    // Access Token 생성
    public String createAccessToken(Long memberId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenExpirationMsec);

        Map<String, Object> claims = new HashMap<>();
        claims.put("memberId", memberId);
        claims.put("type", "ACCESS");

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Refresh Token 생성
    public String createRefreshToken(Long memberId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenExpirationMsec);

        Map<String, Object> claims = new HashMap<>();
        claims.put("memberId", memberId);
        claims.put("type", "REFRESH");

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰에서 MemberId 추출
    public Long getMemberId(String token) {
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.get("memberId").toString());
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    // 토큰에서 Claims 추출
    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    // Secret Key에서 Signing Key 생성
    private Key getKey(String secretKey) {
        byte[] keyBytes = Base64.getDecoder()
                .decode(secretKey.getBytes(StandardCharsets.UTF_8));
        return new javax.crypto.spec.SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.getJcaName());
    }

    // Access Token의 만료 시간(초) 반환
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMsec / 1000;
    }
}
