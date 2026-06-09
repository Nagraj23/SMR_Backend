
package com.spring.smr.Security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {

    private static final String SECRET_KEY = "iUBhYezBVjvKy41I8UvQ1MRRCUJtNR1EaesjJMgAodU";
    private static final long TOKEN_EXPIRY_MS = 24 * 60 * 60 * 1000;

   public String generateToken(UUID userId, String email, String name) {
    return JWT.create()
            .withSubject(email) // 🎯 FIX: Use email as the main username subject
            .withClaim("userId", userId.toString()) // Keep the ID as a separate claim metadata element
            .withClaim("name", name)
            .withIssuedAt(new Date())
            .withExpiresAt(new Date(System.currentTimeMillis() + TOKEN_EXPIRY_MS))
            .sign(Algorithm.HMAC256(SECRET_KEY));
}
}