package com.spring.smr.controller;

import com.spring.smr.dto.*;
import com.spring.smr.service.Authservice;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import java.util.*;
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
        // Alternative fix inside Authcontroller.java (If you want email to remain the subject)
        String verifiedUserEmail = currentUser.getUsername(); // 🎯 No UUID parsing here!nag
        String serviceOutcome = auth.isCompleted(verifiedUserEmail, profileDto, profilePicFile);
        return ResponseEntity.status(HttpStatus.OK).body(serviceOutcome);
    }
@GetMapping("/users/{id}/embedding")
    public ResponseEntity<List<Double>> getPassengerFaceEmbedding(@PathVariable("id") UUID id) {
        // 🎯 Note: If you shifted your Auth Service database lookup entirely to a String email pattern, 
        // you can change the parameter to 'String email' or keep UUID depending on what your service layer uses.
        List<Double> vectorList = auth.getUserEmbedding(id); 
        return ResponseEntity.ok(vectorList);
    }
    
}