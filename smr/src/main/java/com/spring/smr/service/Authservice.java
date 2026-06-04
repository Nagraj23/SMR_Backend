package com.spring.smr.service;

import com.spring.smr.Security.EmailService;
import com.spring.smr.dto.AuthResponse;
import com.spring.smr.dto.LoginDTO;
import com.spring.smr.dto.RegisterDTO;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.spring.smr.entity.Users;
import com.spring.smr.repo.UsersRepository;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor// Generates a constructor ONLY for fields marked final under the hood
public class Authservice {

    // These fields remain final because they are Spring-managed Beans to be injected
    private final UsersRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;


    private Map<String, Boolean> otpVerifiedForReset = new ConcurrentHashMap<>();
    private Map<String, String> otpStore = new ConcurrentHashMap<>();
    private Map<String, Long> otpExpiry = new ConcurrentHashMap<>();

    private static final long OTP_EXPIRY_MS = 2 * 60 * 1000;

    public String generateOTP(String email) {
        String code = String.format("%04d", new Random().nextInt(10000));
        otpStore.put(email, code);
        otpExpiry.put(email, System.currentTimeMillis() + OTP_EXPIRY_MS);
        return code;
    }

    public AuthResponse register(RegisterDTO register) {
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
            emailService.sendOtpEmail(mail, otp);

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
        emailService.sendOtpEmail(mail, otp);

        return AuthResponse.builder()
                .id(savedUser.getUser_id())
                .mail(savedUser.getEmail())
                .name(savedUser.getName())
                .build();
    }

    public AuthResponse login(LoginDTO login) {
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

        return AuthResponse.builder()
                .id(exist.getUser_id())
                .mail(exist.getEmail())
                .name(exist.getName())
                .build();
    }

    public String verifyOtp(String email, String otpInput, String type) {

       String CahcedOtp = otpStore.get(email);
       if(CahcedOtp==null){
           throw  new RuntimeException("NO OTP found for this email");
       }

       Long expiryOtp = otpExpiry.get(email);
       if(expiryOtp <  System.currentTimeMillis()){
           otpStore.remove(email);
           otpExpiry.remove(email);
           throw new RuntimeException("OTP expired");
       }

        if(!CahcedOtp.equals(otpInput)){
            throw new RuntimeException("OTP didnt match ");
        }

        if ("VERIFICATION".equalsIgnoreCase(type)) {
            Users user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("System lookup error: User matching token attributes not found."));

            user.setVerified(true);
            userRepo.save(user);
        } else if ("RESET".equalsIgnoreCase(type)) {
            otpVerifiedForReset.put(email, true);
        }

        otpStore.remove(email);
        otpExpiry.remove(email);

        return "Verification transaction successfully authorized and completed!";
    }

    public String forgot(String email) {
        Optional<Users> user = userRepo.findByEmail(email);

        if (user.isEmpty()) {
            throw new RuntimeException("user does not exist with this mail !");
        }

        String otp = generateOTP(email);
        emailService.sendOtpEmail(email, otp);

        return "OTP has been sent to your email.";
    }

    public boolean reset(String pass, String email) {
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