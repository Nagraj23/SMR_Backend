package com.spring.smr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProfileDTO(
        @NotBlank(message = "Name field cannot be blank or missing")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format. Use international E.164 standard (e.g. +1234567890)")
        String phone,

        @NotEmpty(message = "Biometric face embedding vector array cannot be empty or null")
        @Size(min = 128, max = 512, message = "Face embedding dimensions must be exactly 128 or 512 depending on the network model")
        List<Double> faceEmbedding
) {}
