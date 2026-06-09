package com.spring.smr.dto;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {
    private UUID id;
    private String mail;
    private String name;
    private String token; // 🔐 Added to sign subsequent distributed microservice headers
}