package com.spring.smr.controller;

import com.spring.smr.dto.*;
import com.spring.smr.service.Authservice;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/auth")
public class Authcontroller {

    private final Authservice auth;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterDTO register) {
        AuthResponse response = auth.register(register);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verify(@Valid @RequestBody VerifyRequestDTO verify) {
        AuthResponse response = auth.verifyOtp(verify.getEmail(), verify.getOtp(), verify.getType());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginDTO login) {
        AuthResponse response = auth.login(login);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot")
    public ResponseEntity<String> forgot(@RequestBody ResetDTO payload) {
        String res = auth.forgot(payload.getEmail());
        return ResponseEntity.ok(res);
    }

    @PutMapping("/reset")
    public ResponseEntity<String> reset(@RequestBody ResetDTO payload) {
        boolean res = auth.reset(payload.getPass(), payload.getEmail());
        return ResponseEntity.ok(res ? "Password reset successfully" : "Reset failed");
    }

    @PostMapping(
            value = "/profile/complete",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> finalizeUserProfile(
            @AuthenticationPrincipal UserDetails currentUser,
            @ModelAttribute @Valid ProfileDTO profileDto,
            @RequestParam("file") MultipartFile profilePicFile
    ) {
        UUID verifiedUserId = UUID.fromString(currentUser.getUsername());
        String serviceOutcome = auth.isCompleted(verifiedUserId, profileDto, profilePicFile);
        return ResponseEntity.status(HttpStatus.OK).body(serviceOutcome);
    }

    
}