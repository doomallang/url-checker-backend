package com.doomole.uptime.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expirationSeconds:86400}")
    private long expSec;

    public String generate(Long userId, String email) {
        var now = new java.util.Date();
        var exp = new java.util.Date(now.getTime() + expSec * 1000);
        return io.jsonwebtoken.Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes()),
                        io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }

    public Long verifyAndGetUserId(String token) {
        var jws = io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseClaimsJws(token);
        return Long.valueOf(jws.getBody().getSubject());
    }
}
