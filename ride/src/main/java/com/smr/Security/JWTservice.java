
package com.spring.smr.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class JWTservice {

    public boolean validateToken(String token) {

        try {
            Jwts.parserBuilder()
                    .setSigningKey(getPublicKey())
                    .build()
                    .parseClaimsJws(token);

            return true;

        } catch (JwtException | IllegalArgumentException e) {

            System.err.println(
                    "JWT validation failed: " + e.getMessage()
            );

            return false;
        }
    }

    public String extractUsername(String token) {

        return getClaims(token).getSubject();
    }

    /**
     * Extract userId from JWT.
     */
    public String extractUserId(String token) {

        return getClaims(token)
                .get("userId", String.class);
    }

    /**
     * Extract user's name from JWT.
     */
    public String extractName(String token) {

        return getClaims(token)
                .get("name", String.class);
    }

    /**
     * Parse JWT claims using the RSA public key.
     */
    private Claims getClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(getPublicKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Load RSA public key from:
     *
     * src/main/resources/keys/public_key.pem
     */
    private PublicKey getPublicKey() {

        try {

            ClassPathResource resource =
                    new ClassPathResource("keys/public_key.pem");

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

