package com.spring.smr.Security;

import com.spring.smr.repo.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.spring.smr.Security.JWTfilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JWTfilter jwtAuthFilter;

    private final  UsersRepository userRepo;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http ) throws Exception{
        http
                .csrf(csrf -> csrf.disable())
                // 1. ADD THIS LINE TO ENABLE CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                         .requestMatchers(
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-ui.html"
            ).permitAll()
            .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/auth/users/**").permitAll()
            .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/auth/users/*/embedding").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();

        // Allow your React frontend URL
        configuration.setAllowedOrigins(java.util.List.of("http://localhost:5173", "http://localhost:3000","http://10.73.213.135:8080"));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            // 1. Fetch your clean domain database record
            com.spring.smr.entity.Users domainUser = this.userRepo.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

            // 2. Adapt and map it straight into Spring Security's native model class type
            return org.springframework.security.core.userdetails.User.builder()
                    .username(domainUser.getEmail())
                    .password(domainUser.getPassword()) // Automatically prepends "ROLE_"
                    .build();
        };
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                // Tells the engine to ignore authentication completely for the inter-service embedding route
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/auth/users/**");
    }
}
