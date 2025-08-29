package de.saarland.events.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import de.saarland.events.dto.payment.CreatePaymentRequest;
import de.saarland.events.dto.payment.CreatePaymentResponse;
import de.saarland.events.security.services.UserDetailsImpl;
import de.saarland.events.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-checkout-session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CreatePaymentResponse> createCheckoutSession(@Valid @RequestBody CreatePaymentRequest request, Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            Long userId = userDetails.getId();
            request.setUserId(userId);

            Session session = paymentService.createStripeSession(request);
            return ResponseEntity.ok(new CreatePaymentResponse(session.getUrl()));
        } catch (StripeException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
}
