package com.spring.smr.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyRequestDTO {
    private String email;
    private String otp;
    private String type;
}