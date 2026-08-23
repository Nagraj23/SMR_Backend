package com.spring.smr.service;

import com.spring.smr.Security.EmailService;
import com.spring.smr.Security.JWTservice;
import com.spring.smr.dto.AuthResponse;
import com.spring.smr.dto.LoginDTO;
import com.spring.smr.dto.ProfileDTO;
import com.spring.smr.dto.RegisterDTO;
import com.spring.smr.entity.Users;
import com.spring.smr.exception.ResourceNotFoundException;
import com.spring.smr.repo.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class Authservice {

    private final UsersRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JWTservice jwtService;
    private final WebClient webClient;

    private final Map<String, Boolean> otpVerifiedForReset = new ConcurrentHashMap<>();
    private final Map<String, String> otpStore = new ConcurrentHashMap<>();
    private final Map<String, Long> otpExpiry = new ConcurrentHashMap<>();

    private static final long OTP_EXPIRY_MS = 2 * 60 * 1000;

    public String generateOTP(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Cannot generate OTP: Email address cannot be blank.");
        }
        String code = String.format("%04d", new Random().nextInt(10000));
        otpStore.put(email, code);
        otpExpiry.put(email, System.currentTimeMillis() + OTP_EXPIRY_MS);
        return code;
    }

    public AuthResponse register(RegisterDTO register) {
        if (register == null || register.getMail() == null || register.getMail().isBlank()) {
            throw new IllegalArgumentException("Registration payload and email cannot be blank.");
        }

        String mail = register.getMail();
        log.info("📝 [REGISTER ATTEMPT] Processing registration for: {}", mail);

        Optional<Users> exist = userRepo.findByEmail(mail);

        if (exist.isPresent()) {
            Users existingUser = exist.get();
            if (existingUser.isVerified()) {
                log.warn("⛔ [REGISTER BLOCKED] Email already registered and verified: {}", mail);
                throw new IllegalArgumentException("Email already registered and verified!");
            }

            existingUser.setName(register.getName());
            existingUser.setPassword(passwordEncoder.encode(register.getPassword()));
            Users savedUser = userRepo.save(existingUser);

            String otp = generateOTP(mail);
            log.info("🎯 LOCAL TEST OTP FOR EXISTING USER [{}] -> {}", mail, otp);

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
        log.info("🎯 LOCAL TEST OTP FOR NEW USER [{}] -> {}", mail, otp);

        return AuthResponse.builder()
                .id(savedUser.getUser_id())
                .mail(savedUser.getEmail())
                .name(savedUser.getName())
                .build();
    }

    public AuthResponse login(LoginDTO login) {
        if (login == null || login.getMail() == null) {
            throw new IllegalArgumentException("Login payload and credentials are required.");
        }

        log.info("🔑 [LOGIN ATTEMPT] Authenticating: {}", login.getMail());

        Users exist = userRepo.findByEmail(login.getMail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + login.getMail()));

        if (!exist.isVerified()) {
            log.warn("⛔ [LOGIN BLOCKED] Unverified account attempt: {}", login.getMail());
            throw new IllegalArgumentException("Please verify your email address before logging in!");
        }

        if (!passwordEncoder.matches(login.getPassword(), exist.getPassword())) {
            log.warn("⛔ [INVALID CREDENTIALS] Bad password for: {}", login.getMail());
            throw new IllegalArgumentException("Invalid email or password credentials!");
        }

        String token = jwtService.generateToken(
                exist.getEmail(),
                exist.getUser_id(),
                exist.getName()
        );

        log.info("✅ [LOGIN SUCCESS] JWT issued for userId: {}", exist.getUser_id());

        return AuthResponse.builder()
                .id(exist.getUser_id())
                .mail(exist.getEmail())
                .name(exist.getName())
                .token(token)
                .build();
    }

    public AuthResponse verifyOtp(String email, String otpInput, String type) {
        if (email == null || otpInput == null) {
            throw new IllegalArgumentException("Email and OTP fields cannot be null.");
        }

        log.info("🔍 [VERIFY OTP] Validating code for: {} | Type: {}", email, type);

        String cachedOtp = otpStore.get(email);
        if (cachedOtp == null) {
            throw new IllegalArgumentException("No active OTP found for this email.");
        }

        Long expiryOtp = otpExpiry.get(email);
        if (expiryOtp == null || expiryOtp < System.currentTimeMillis()) {
            otpStore.remove(email);
            otpExpiry.remove(email);
            throw new IllegalArgumentException("OTP has expired. Please request a new code.");
        }

        if (!cachedOtp.equals(otpInput)) {
            log.warn("⛔ [OTP MISMATCH] Incorrect OTP provided for: {}", email);
            throw new IllegalArgumentException("Invalid OTP code.");
        }

        Users user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if ("VERIFICATION".equalsIgnoreCase(type)) {
            user.setVerified(true);
            user = userRepo.save(user);
            log.info("✅ [ACCOUNT VERIFIED] User {} marked verified.", email);
        } else if ("RESET".equalsIgnoreCase(type)) {
            otpVerifiedForReset.put(email, true);
            log.info("✅ [RESET UNLOCKED] Password reset unlocked for: {}", email);
        }

        otpStore.remove(email);
        otpExpiry.remove(email);

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getUser_id(),
                user.getName()
        );

        return AuthResponse.builder()
                .id(user.getUser_id())
                .mail(user.getEmail())
                .name(user.getName())
                .token(token)
                .build();
    }

    @Transactional
    public String isCompleted(String email, ProfileDTO profile, MultipartFile file) {
        log.info("🖼️ [PROFILE COMPLETE] Uploading biometric embedding for: {}", email);

        Users userProfile = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (!userProfile.isVerified()) {
            throw new IllegalArgumentException("Account must be verified before completing profile.");
        }

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", file.getResource());

        List<Double> extractedVectors;
        try {
            extractedVectors = webClient.post()
                    .uri("/verify/extract")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Double>>() {})
                    .block();
        } catch (Exception ex) {
            log.error("💥 [PYTHON AI OFFLINE] AI extraction failed for {}: {}", email, ex.getMessage());
            throw new RuntimeException("Biometric Vision Server unavailable. Please try again later.");
        }

        if (extractedVectors == null || extractedVectors.isEmpty()) {
            throw new IllegalArgumentException("Face not recognized. Please submit a clearer image.");
        }

        userProfile.setName(profile.name());
        userProfile.setPhone(profile.phone());
        userProfile.setFaceEmbedding(extractedVectors);
        userProfile.setProfileStatus("VERIFIED");

        userRepo.save(userProfile);
        log.info("✅ [PROFILE STORED] Biometric vectors synced (Dimensions: {})", extractedVectors.size());

        return "Profile registration finalized successfully.";
    }

    @Transactional(readOnly = true)
    public List<Double> getUserEmbeddingById(UUID userId) {
        Users user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getFaceEmbedding() == null || user.getFaceEmbedding().isEmpty()) {
            log.warn("⚠️ [EMBEDDING EMPTY] No biometric vectors stored for userId: {}", userId);
            return Collections.emptyList();
        }

        return user.getFaceEmbedding();
    }

    public String forgot(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email address is required.");
        }

        if (!userRepo.existsByEmail(email)) {
            throw new ResourceNotFoundException("No account registered with email: " + email);
        }

        String otp = generateOTP(email);
        emailService.sendOtpEmail(email, otp);
        log.info("📧 [OTP SENT] Reset code dispatched for: {}", email);

        return "OTP has been sent to your email.";
    }

    public boolean reset(String pass, String email) {
        if (email == null || pass == null || pass.isBlank()) {
            throw new IllegalArgumentException("Email and password parameters cannot be blank.");
        }

        Boolean isVerified = otpVerifiedForReset.get(email);
        if (isVerified == null || !isVerified) {
            log.warn("⛔ [RESET REJECTED] Unverified password reset attempt for: {}", email);
            throw new IllegalArgumentException("Unauthorized: OTP verification required before resetting password.");
        }

        Users existingUser = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        existingUser.setPassword(passwordEncoder.encode(pass));
        userRepo.save(existingUser);
        otpVerifiedForReset.remove(email);

        log.info("✅ [PASSWORD UPDATED] Password reset successful for: {}", email);
        return true;
    }
}