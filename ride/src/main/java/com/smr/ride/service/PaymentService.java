package com.smr.ride.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.smr.ride.entity.Payment;
import com.smr.ride.entity.Payment.PaymentMode;
import com.smr.ride.entity.Payment.PaymentStatus;
import com.smr.ride.repo.PaymentRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpayClient;

    // Injecting real developer test keys directly from application.properties
    public PaymentService(PaymentRepository paymentRepository,
                          @Value("${razorpay.key.id}") String keyId,
                          @Value("${razorpay.key.secret}") String keySecret) throws Exception {
        this.paymentRepository = paymentRepository;
        // Initializes a live connection pool straight to Razorpay's global test servers
        this.razorpayClient = new RazorpayClient(keyId, keySecret);
    }

    @Transactional
    public Payment createPendingPayment(UUID rideId, UUID passengerId, BigDecimal amount, PaymentMode mode) {
        Payment payment = new Payment();
        payment.setRideId(rideId);
        payment.setPassengerId(passengerId);
        payment.setAmount(amount);
        payment.setPaymentMode(mode);
        payment.setStatus(PaymentStatus.PENDING);

        if (mode == PaymentMode.NETBANKING) {
            try {
                // IMPORTANT: Razorpay processes money in currency subunits (Paise for INR). Multiply by 100!
                int amountInPaise = amount.multiply(new BigDecimal("100")).intValue();

                JSONObject orderRequest = new JSONObject();
                orderRequest.put("amount", amountInPaise);
                orderRequest.put("currency", "INR");
                orderRequest.put("receipt", rideId.toString());

                // 🚀 REAL NETWORK HOP: Transmit the payment contract straight to Razorpay's servers
                Order externalOrder = razorpayClient.orders.create(orderRequest);

                // Extract the real cloud order session ID (e.g., order_Kxp23Lmn987)
                payment.setRazorpayOrderId(externalOrder.get("id"));
            } catch (Exception e) {
                throw new RuntimeException("Failed to construct live Razorpay order session, bro!", e);
            }
        }

        return paymentRepository.save(payment);
    }

    @Transactional
    public void settlePaymentLocally(UUID rideId) {
        Payment payment = paymentRepository.findByRideId(rideId)
                .orElseThrow(() -> new RuntimeException("Ledger record not found for this ride context!"));

        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);
    }

    public UUID getRideIdFromOrder(String razorpayOrderId) {
        return paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .map(Payment::getRideId)
                .orElseThrow(() -> new RuntimeException("No localized ledger match found for order: " + razorpayOrderId));
    }
}