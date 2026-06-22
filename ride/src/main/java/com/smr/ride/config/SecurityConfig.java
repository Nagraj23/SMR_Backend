package com.smr.ride.config;

import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // Pulls your shared secret key string straight from your application.properties file
    @Value("${spring.security.oauth2.resourceserver.jwt.secret-key-string}")
    private String secretKeyString;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/rides/create").authenticated()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/rides/*/book").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));

        return http.build();
    }

    /**
     * FIXED BEAN: Explicitly defines the JwtDecoder using an HMAC-SHA256 signature algorithm.
     * This supplies the missing dependency to WebSecurityConfiguration.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // Build a type-safe secret key spec instance out of your raw config string bytes
        SecretKeySpec secretKey = new SecretKeySpec(
                this.secretKeyString.getBytes(),
                "HmacSHA256"
        );

        // Return a localized Nimbus decoder configured to verify incoming header barcodes offline
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}