package com.spring.smr.Security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JWTservice {

    @Value("${app.security.jwt.private-key-path}")
    private String privateKeyPath;

    @Value("${app.security.jwt.public-key-path}")
    private String publicKeyPath;

    @Value("${app.security.jwt.expiration-ms}")
    private long EXPIRATION;

    public String generateToken(String username, UUID userId, String name) {

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId.toString())
                .claim("name", name)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    public String extractUsername(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(getPublicKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {

        try {
            Jwts.parserBuilder()
                    .setSigningKey(getPublicKey())
                    .build()
                    .parseClaimsJws(token);

            return true;

        } catch (JwtException | IllegalArgumentException e) {

            System.err.println("JWT Validation Error: " + e.getMessage());
            return false;
        }
    }

    private PrivateKey getPrivateKey() {

        try {

            String key = Files.readString(Path.of(privateKeyPath));

            key = key
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(key);

            PKCS8EncodedKeySpec keySpec =
                    new PKCS8EncodedKeySpec(decoded);

            KeyFactory keyFactory =
                    KeyFactory.getInstance("RSA");

            return keyFactory.generatePrivate(keySpec);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Could not load RSA private key",
                    e
            );
        }
    }

    private PublicKey getPublicKey() {

        try {

            String key = Files.readString(Path.of(publicKeyPath));

            key = key
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(key);

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