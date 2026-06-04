package com.spring.smr.Security; // Fixed package string to match folder destination

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            // 1. Create a raw MIME internet envelope instance
            MimeMessage message = mailSender.createMimeMessage();

            // 2. Wrap it inside a Multi-part UTF-8 stream helper layout
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 3. Assign network routing header variables
            helper.setTo(toEmail);
            helper.setSubject("SMR Platform - Verify Your Identity");

            // 4. Construct high-visibility structured HTML visualization blocks
            String htmlContent = """
                <div style='font-family: Arial, sans-serif; padding: 20px; border: 1px solid #eee;'>
                    <h2 style='color: #2F80ED;'>Welcome to SMR Ride-Sharing!</h2>
                    <p>Thank you for registering. Please use the following One-Time Password (OTP) to verify your account profile:</p>
                    <div style='font-size: 24px; font-weight: bold; background: #f2f2f2; padding: 10px 20px; display: inline-block; letter-spacing: 4px; color: #333;'>
                        %s
                    </div>
                    <p style='margin-top: 20px; color: #777; font-size: 12px;'>This code is highly sensitive and will expire in 2 minutes.</p>
                </div>
            """.formatted(otpCode);

            // 5. Release the formatted stream to the mail layout helper
            helper.setText(htmlContent, true);

            // 6. Push the token out to the web networks offline
            mailSender.send(message);

            System.out.println("SUCCESS -> Asynchronous OTP email dispatched to: " + toEmail);
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR -> Failed to process background mail worker: " + e.getMessage());
        }
    }
}