// Fichier: src/main/java/com/utils/JwtUtil.java
package com.utils;

import com.exceptions.AuthException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import com.models.User;
import com.demo.enums.Role;

public class JwtUtil {
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long EXPIRATION_TIME = 86400000; // 24 heures

    public static String generateToken(User user) {
        return Jwts.builder()
                .claim("userId", user.getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }

    public static Claims parseToken(String token) throws JwtException {
        // Vérifie d'abord la blacklist
        if (JwtBlacklist.isTokenBlacklisted(token)) {
            throw new JwtException("Token invalide (déconnecté)");
        }

        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}