package de.saarland.events.controller;

import com.stripe.model.Event;
import com.stripe.net.Webhook;
import de.saarland.events.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.stripe.exception.SignatureVerificationException;

@RestController
@RequestMapping("/api/payments/stripe")
public class StripeWebhookController {

    private final PaymentService paymentService;

    @Value("${STRIPE_WEBHOOK_SECRET}")
    private String webhookSecret;

    public StripeWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/events")
    public ResponseEntity<String> handleStripeEvents(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        if (webhookSecret == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook secret not configured.");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature.");
        }

        paymentService.handleStripeEvent(event);

        return new ResponseEntity<>("Success", HttpStatus.OK);
    }
}
