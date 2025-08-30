package de.saarland.events.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import de.saarland.events.dto.payment.CreatePaymentRequest;
import de.saarland.events.model.EPaymentStatus;
import de.saarland.events.model.PaymentOrder;
import de.saarland.events.model.User;
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

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final EmailService emailService;

    private static final Map<Integer, Long> TARIFFS = Map.of(
            3, 1000L,
            7, 2000L,
            14, 3000L,
            30, 5000L
    );

    public PaymentService(EventRepository eventRepository, UserRepository userRepository, PaymentOrderRepository paymentOrderRepository, EmailService emailService) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.emailService = emailService;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Transactional
    public Session createStripeSession(CreatePaymentRequest request) throws StripeException {
        // Этот метод мы не меняем, он работает правильно
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        de.saarland.events.model.Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        if (event.getStatus() != de.saarland.events.model.EStatus.APPROVED) {
            throw new IllegalArgumentException("Event is not approved for promotion.");
        }
        if (event.isPremium()) {
            throw new IllegalArgumentException("This event is already promoted.");
        }
        Long priceInCents = TARIFFS.get(request.getDays());
        if (priceInCents == null) {
            throw new IllegalArgumentException("Invalid promotion duration specified.");
        }
        paymentOrderRepository.findByEventIdAndStatus(request.getEventId(), EPaymentStatus.PENDING)
                .ifPresent(paymentOrderRepository::delete);
        String eventName = event.getTranslations().stream()
                .filter(t -> "de".equals(t.getLocale()))
                .findFirst()
                .orElse(event.getTranslations().get(0))
                .getName();
        PaymentOrder order = new PaymentOrder();
        order.setUser(user);
        order.setEvent(event);
        order.setAmount(priceInCents);
        order.setStatus(EPaymentStatus.PENDING);
        order.setCurrency("eur");
        order.setPromotionDays(request.getDays());
        order.setCreatedAt(ZonedDateTime.now());
        PaymentOrder savedOrder = paymentOrderRepository.save(order);
        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.PAYPAL)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://www.saarland-events-new.de/payment-success")
                .setCancelUrl("https://www.saarland-events-new.de/payment-cancel")
                .putMetadata("orderId", savedOrder.getId().toString())
                .setCustomerEmail(user.getEmail())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount(priceInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Promotion for: " + eventName)
                                                                .setDescription(request.getDays() + " days of premium placement")
                                                                .build())
                                                .build())
                                .build())
                .build();
        Session session = Session.create(params);
        savedOrder.setStripeSessionId(session.getId());
        paymentOrderRepository.save(savedOrder);
        return session;
    }


    @Transactional
    public void handleStripeEvent(com.stripe.model.Event stripeEvent) {
        logger.info("Received Stripe event: type = {}", stripeEvent.getType());

        if ("checkout.session.completed".equals(stripeEvent.getType())) {
            logger.info("Processing checkout.session.completed event.");

            EventDataObjectDeserializer dataObjectDeserializer = stripeEvent.getDataObjectDeserializer();
            if (dataObjectDeserializer.getObject().isEmpty()) {
                logger.error("Stripe event data object is empty. Cannot process webhook.");
                return;
            }

            StripeObject stripeObject = dataObjectDeserializer.getObject().get();
            Session session = (Session) stripeObject;

            logger.info("Stripe session ID from webhook: {}", session.getId());

            PaymentOrder order = paymentOrderRepository.findByStripeSessionId(session.getId()).orElse(null);

            if (order == null) {
                logger.error("PaymentOrder with Stripe Session ID {} not found in the database.", session.getId());
                return;
            }

            logger.info("Found PaymentOrder with ID {}. Current status: {}", order.getId(), order.getStatus());

            if (order.getStatus() == EPaymentStatus.PENDING) {
                logger.info("Order status is PENDING. Updating event to premium...");

                order.setStatus(EPaymentStatus.PAID);

                de.saarland.events.model.Event event = order.getEvent();
                event.setPremium(true);
                event.setPremiumUntil(ZonedDateTime.now().plusDays(order.getPromotionDays()));

                eventRepository.save(event);
                paymentOrderRepository.save(order);
                logger.info("Successfully updated Event ID {} to premium. New status for Order ID {} is PAID.", event.getId(), order.getId());

                try {
                    emailService.sendPromotionConfirmationEmail(order.getUser(), event);
                    logger.info("Promotion confirmation email sent to {}.", order.getUser().getEmail());
                } catch (Exception e) {
                    logger.error("Failed to send promotion confirmation email.", e);
                }
            } else {
                logger.warn("Order with ID {} was already processed. Current status: {}. No action taken.", order.getId(), order.getStatus());
            }
        }
    }
}