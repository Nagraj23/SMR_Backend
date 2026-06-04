package com.spring.smr.controller;



import com.spring.smr.dto.*;
import com.spring.smr.repo.UsersRepository;
import com.spring.smr.service.Authservice;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/auth")
public class Authcontroller {

    private final Authservice auth ;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterDTO register){

        AuthResponse response = auth.register(register);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verify (@RequestBody VerifyRequestDTO verify ){

        String outcome = auth.verifyOtp(verify.getEmail(), verify.getOtp(), verify.getType());
        return ResponseEntity.ok(outcome);
    }

    @PostMapping("/login")
    public  ResponseEntity<AuthResponse> login(@RequestBody LoginDTO login){
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
}
