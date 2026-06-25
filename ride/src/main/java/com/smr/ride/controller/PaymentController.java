package com.smr.ride.controller;

import com.smr.ride.service.RideService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final RideService rideService;

    // Injecting your existing RideService to reuse the Phase B closeout logic
    public PaymentController(RideService rideService) {
        this.rideService = rideService;
    }

    /**
     * 🎯 Asynchronous S2S Webhook Entry Point
     * Decouples checkout completion from the mobile client token lifecycle.
     */
    @PostMapping("/webhook/mock-bank")
    public ResponseEntity<String> handleMockBankWebhook(@RequestBody Map<String, Object> payload) {
        try {
            // 1. Extract the event type from Razorpay's body structure
            String event = (String) payload.get("event");

            if ("payment.captured".equals(event)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> innerPayload = (Map<String, Object>) payload.get("payload");
                String orderId = (String) innerPayload.get("order_id");

                if (orderId == null) {
                    return ResponseEntity.badRequest().body("Missing structural tracking order_id, bro!");
                }

                // 2. Resolve our database transaction map mapping shortcut via RideService
                UUID rideId = rideService.findRideIdByRazorpayOrder(orderId);

                // 3. Trigger Phase B to close out the ride and booking status rows natively
                String archivalOutcome = rideService.settleAndCloseRide(rideId);

                return ResponseEntity.ok("Async payment cleared. " + archivalOutcome);
            }

            return ResponseEntity.ok("Event ignored");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Webhook Processing Failure: " + e.getMessage());
        }
    }
}