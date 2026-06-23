package com.spring.smr.service;

import com.spring.smr.Security.EmailService;
import com.spring.smr.Security.JwtProvider;
import com.spring.smr.dto.AuthResponse;
import com.spring.smr.dto.LoginDTO;
import com.spring.smr.dto.ProfileDTO;
import com.spring.smr.dto.RegisterDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.spring.smr.entity.Users;
import com.spring.smr.Security.JwtProvider;
import com.spring.smr.repo.UsersRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.core.ParameterizedTypeReference;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class Authservice {

    private final UsersRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtProvider jwtProvider;
    private final WebClient webClient;

    private final Map<String, Boolean> otpVerifiedForReset = new ConcurrentHashMap<>();
    private final Map<String, String> otpStore = new ConcurrentHashMap<>();
    private final Map<String, Long> otpExpiry = new ConcurrentHashMap<>();

    private static final long OTP_EXPIRY_MS = 2 * 60 * 1000;

    public String generateOTP(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Cannot generate OTP: Email address argument value is null.");
        }
        String code = String.format("%04d", new Random().nextInt(10000));
        otpStore.put(email, code);
        otpExpiry.put(email, System.currentTimeMillis() + OTP_EXPIRY_MS);
        return code;
    }

    public AuthResponse register(RegisterDTO register) {
        if (register == null || register.getMail() == null) {
            throw new RuntimeException("Registration invalid: Registration payload and email properties cannot be blank.");
        }

        String mail = register.getMail();
        Optional<Users> exist = userRepo.findByEmail(mail);

        if (exist.isPresent()) {
            Users existingUser = exist.get();
            if (existingUser.isVerified()) {
                throw new RuntimeException("Email already registered and verified!");
            }

            existingUser.setName(register.getName());
            existingUser.setPassword(passwordEncoder.encode(register.getPassword()));
            Users savedUser = userRepo.save(existingUser);

            String otp = generateOTP(mail);
            
            // 🎯 BYPASS HANDSHAKE: Print directly to console to bypass network timeouts
            System.out.println("=================================================");
            System.out.println("🎯 LOCAL TEST OTP FOR EXISTING USER -> " + otp);
            System.out.println("=================================================");

            return AuthResponse.builder()
                    .id(savedUser.getUser_id())
                    .mail(savedUser.getEmail())
                    .name(savedUser.getName())
                    .build();
        }

        String encryptedPassword = passwordEncoder.encode(register.getPassword());
        Users newUser = Users.builder()
                .email(mail)
                .name(register.getName())
                .isVerified(false)
                .password(encryptedPassword)
                .build();

        Users savedUser = userRepo.save(newUser);

        String otp = generateOTP(mail);
        
        // 🎯 BYPASS HANDSHAKE: Print directly to console to bypass network timeouts
        System.out.println("=================================================");
        System.out.println("🎯 LOCAL TEST OTP FOR NEW USER -> " + otp);
        System.out.println("=================================================");

        return AuthResponse.builder()
                .id(savedUser.getUser_id())
                .mail(savedUser.getEmail())
                .name(savedUser.getName())
                .build();
    }

    public AuthResponse login(LoginDTO login) {
        if (login == null || login.getMail() == null) {
            throw new RuntimeException("Authentication failed: Login payload and credentials fields are required.");
        }

        if (!userRepo.existsByEmail(login.getMail())) {
            throw new RuntimeException("User not found with this mail!");
        }

        Users exist = userRepo.findByEmail(login.getMail())
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (!exist.isVerified()) {
            throw new RuntimeException("Please verify your email address before logging in!");
        }

        if (!passwordEncoder.matches(login.getPassword(), exist.getPassword())) {
            throw new RuntimeException("Invalid email or password credentials!");
        }

        String token = jwtProvider.generateToken(exist.getUser_id(), exist.getEmail(), exist.getName());

        return AuthResponse.builder()
                .id(exist.getUser_id())
                .mail(exist.getEmail())
                .name(exist.getName())
                .token(token)
                .build();
    }

    public AuthResponse verifyOtp(String email, String otpInput, String type) {
        if (email == null || otpInput == null) {
            throw new RuntimeException("Verification request failed: Email parameter fields cannot be null.");
        }

        String CachedOtp = otpStore.get(email);
        if (CachedOtp == null) {
            throw new RuntimeException("NO OTP found for this email");
        }

        Long expiryOtp = otpExpiry.get(email);
        if (expiryOtp == null || expiryOtp < System.currentTimeMillis()) {
            otpStore.remove(email);
            otpExpiry.remove(email);
            throw new RuntimeException("OTP expired");
        }

        if (!CachedOtp.equals(otpInput)) {
            throw new RuntimeException("OTP didn't match");
        }

        Users user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("System lookup error: User matching token attributes not found."));

        if ("VERIFICATION".equalsIgnoreCase(type)) {
            user.setVerified(true);
            user = userRepo.save(user);
        } else if ("RESET".equalsIgnoreCase(type)) {
            otpVerifiedForReset.put(email, true);
        }

        otpStore.remove(email);
        otpExpiry.remove(email);

       String token = jwtProvider.generateToken(user.getUser_id(), user.getEmail(), user.getName());

        return AuthResponse.builder()
                .id(user.getUser_id())
                .mail(user.getEmail())
                .name(user.getName())
                .token(token)
                .build();
    }

    @org.springframework.transaction.annotation.Transactional
    public String isCompleted(String email, ProfileDTO profile, MultipartFile file){

        Users userProfile = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("System lookup error: User matching email attributes not found."));

        if (!userProfile.isVerified()) {
            throw new RuntimeException("user is not verified");
        }

        org.springframework.http.client.MultipartBodyBuilder bodyBuilder = new org.springframework.http.client.MultipartBodyBuilder();
        bodyBuilder.part("file", file.getResource());

        // Call the Python extractor
        List<Double> extractedVectors = webClient.post()
                .uri("/verify/extract") 
                .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA) // Force multipart formatting
                .body(org.springframework.web.reactive.function.BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<List<Double>>() {})
                .block();

        // 🚀 DIAGNOSTIC LOG: Print to console so we can verify the payload vector isn't empty!
        System.out.println("\n================= 🎯 AUTH VECTOR SAVE DEBUG =================");
        System.out.println("Extracted Vector Size From Python: " + (extractedVectors != null ? extractedVectors.size() : "NULL"));
        System.out.println("Target User Email Row             : " + email);
        System.out.println("=============================================================\n");

        if (extractedVectors == null || extractedVectors.isEmpty()) {
            throw new RuntimeException("AI Extraction Failure: Python returned an uninitialized vector payload.");
        }

        userProfile.setName(profile.name());
        userProfile.setPhone(profile.phone());
        userProfile.setFaceEmbedding(extractedVectors); // Drops the float collection array safely
        userProfile.setProfileStatus("VERIFIED");

        Users updatedUserInstance = userRepo.saveAndFlush(userProfile);

        // Double-check logging to confirm memory assignment state
        System.out.println("Row finalized cleanly. Total elements synced to memory tracking: " 
            + updatedUserInstance.getFaceEmbedding().size());

        userRepo.save(userProfile);

        return "Profile registration finalized successfully. Account status mutated to VERIFIED.";
    }

  @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Double> getUserEmbeddingById(java.util.UUID userId) {
        // 🎯 1. Use findById to capture the fresh proxy reference context
        Users user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("System lookup error: User matching id attributes not found."));

        // 🎯 2. Explicitly invoke size() to force Hibernate to lazy-load the collection table records right now!
        if (user.getFaceEmbedding() != null) {
            user.getFaceEmbedding().size(); 
        }

        // 🎯 3. Return a clean fallback array check instead of throwing a hard crash immediately
        if (user.getFaceEmbedding() == null || user.getFaceEmbedding().isEmpty()) {
            System.out.println("⚠️ WARN: Database collection table read empty for User ID: " + userId);
            return java.util.Collections.emptyList(); // Return empty array instead of crashing!
        }

        return user.getFaceEmbedding();
    }
    
    public String forgot(String email) {
        if (email == null) {
            throw new RuntimeException("Forgot password request failed: Missing target email address.");
        }

        Optional<Users> user = userRepo.findByEmail(email);
        if (user.isEmpty()) {
            throw new RuntimeException("user does not exist with this mail !");
        }

        String otp = generateOTP(email);
        emailService.sendOtpEmail(email, otp);

        return "OTP has been sent to your email.";
    }

    public boolean reset(String pass, String email) {
        if (email == null || pass == null) {
            throw new RuntimeException("Password mutation rejected: Missing target transactional parameters.");
        }

        Boolean isVerified = otpVerifiedForReset.get(email);
        if (isVerified == null || !isVerified) {
            throw new RuntimeException("Unauthorized: OTP verification required before resetting password.");
        }

        Optional<Users> user = userRepo.findByEmail(email);
        if (user.isEmpty()) {
            throw new RuntimeException("user doesnt exist ");
        }

        String password = passwordEncoder.encode(pass);
        Users existingUser = user.get();
        existingUser.setPassword(password);

        userRepo.save(existingUser);
        otpVerifiedForReset.remove(email);

        return true;
    }
}