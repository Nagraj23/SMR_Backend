package com.smr.notify.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

@Service
public class JWTservice {


    public Claims extractClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(getPublicKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    public UUID extractUserId(String token) {

        Claims claims = extractClaims(token);

        String userId = claims.get("userId", String.class);

        if (userId == null) {
            throw new JwtException("userId missing from JWT");
        }

        return UUID.fromString(userId);
    }


    public boolean validateToken(String token) {

        try {

            extractClaims(token);

            return true;

        } catch (JwtException | IllegalArgumentException e) {

            return false;
        }
    }


    private PublicKey getPublicKey() {

        try {

            ClassPathResource resource =
                    new ClassPathResource("keys/public.key");

            String key = new String(
                    resource.getInputStream().readAllBytes()
            );

            key = key
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded =
                    Base64.getDecoder().decode(key);

            X509EncodedKeySpec keySpec =
                    new X509EncodedKeySpec(decoded);

            KeyFactory keyFactory =
                    KeyFactory.getInstance("RSA");

            return keyFactory.generatePublic(keySpec);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Could not load RSA public key",
                    e
            );
        }
    }
}