package de.saarland.events.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import de.saarland.events.dto.payment.CreatePaymentRequest;
import de.saarland.events.model.*;
import de.saarland.events.repository.EventRepository;
import de.saarland.events.repository.PaymentOrderRepository;
import de.saarland.events.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${STRIPE_SECRET_KEY}")
    private String stripeSecretKey;

    @Value("${app.oauth2.redirectUri}")
    private String clientBaseUrl;

    private final PaymentOrderRepository paymentOrderRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final Map<Integer, Long> PRICE_MAP = Map.of(
            3, 1000L,
            7, 2000L,
            14, 3000L,
            30, 5000L
    );

    public PaymentService(PaymentOrderRepository paymentOrderRepository, EventRepository eventRepository, UserRepository userRepository, EmailService emailService) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Transactional
    public Session createPaymentSession(CreatePaymentRequest request, Long userId) throws StripeException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + request.getEventId()));

        if (event.getStatus() != EStatus.APPROVED) {
            throw new IllegalArgumentException("Only approved events can be promoted.");
        }

        if (event.isPremium()) {
            throw new IllegalArgumentException("This event is already premium.");
        }

        Long amount = PRICE_MAP.get(request.getDays());
        if (amount == null) {
            throw new IllegalArgumentException("Invalid promotion duration: " + request.getDays() + " days.");
        }

        PaymentOrder order = new PaymentOrder();
        order.setUser(user);
        order.setEvent(event);
        order.setAmount(amount);
        order.setCurrency("eur");
        order.setStatus(EPaymentStatus.PENDING);
        order.setCreatedAt(ZonedDateTime.now());
        PaymentOrder savedOrder = paymentOrderRepository.save(order);

        String successUrl = clientBaseUrl + "/profile?payment=success";
        String cancelUrl = clientBaseUrl + "/profile?payment=cancel";

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount(amount)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Premium Promotion for '" + event.getTranslations().getFirst().getName() + "'")
                                                                .setDescription(request.getDays() + " days of promotion")
                                                                .build())
                                                .build())
                                .build())
                .putMetadata("orderId", savedOrder.getId().toString())
                .putMetadata("eventId", event.getId().toString())
                .putMetadata("promotionDays", String.valueOf(request.getDays()))
                .build();

        Session session = Session.create(params);
        savedOrder.setStripeSessionId(session.getId());
        paymentOrderRepository.save(savedOrder);

        return session;
    }

    @Transactional
    public void handleStripeEvent(com.stripe.model.Event stripeEvent) {
        if ("checkout.session.completed".equals(stripeEvent.getType())) {
            Session session = (Session) stripeEvent.getDataObjectDeserializer().getObject().orElse(null);
            if (session != null) {
                String sessionId = session.getId();
                Optional<PaymentOrder> orderOptional = paymentOrderRepository.findByStripeSessionId(sessionId);

                if (orderOptional.isPresent()) {
                    PaymentOrder order = orderOptional.get();
                    if (order.getStatus() == EPaymentStatus.PENDING) {
                        order.setStatus(EPaymentStatus.PAID);
                        order.setUpdatedAt(ZonedDateTime.now());
                        paymentOrderRepository.save(order);

                        Event eventToPromote = order.getEvent();
                        int promotionDays = Integer.parseInt(session.getMetadata().get("promotionDays"));
                        eventToPromote.setPremium(true);
                        eventToPromote.setPremiumUntil(ZonedDateTime.now().plusDays(promotionDays));
                        eventRepository.save(eventToPromote);


                        emailService.sendPremiumActivationEmail(order.getUser(), eventToPromote);
                        logger.info("Event {} promoted for {} days.", eventToPromote.getId(), promotionDays);
                    }
                } else {
                    logger.warn("Received checkout.session.completed for unknown session id: {}", sessionId);
                }
            }
        }
    }
}
