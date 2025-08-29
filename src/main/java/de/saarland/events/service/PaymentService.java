package de.saarland.events.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Map;

@Service
public class PaymentService {

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
    public void handleStripeEvent(Event stripeEvent) {
        if ("checkout.session.completed".equals(stripeEvent.getType())) {
            Session session = (Session) stripeEvent.getDataObjectDeserializer().getObject().orElse(null);
            if (session != null) {
                String orderIdStr = session.getMetadata().get("orderId");
                if (orderIdStr != null) {
                    Long orderId = Long.parseLong(orderIdStr);
                    PaymentOrder order = paymentOrderRepository.findById(orderId)
                            .orElseThrow(() -> new EntityNotFoundException("PaymentOrder not found: " + orderId));

                    if (order.getStatus() == EPaymentStatus.PENDING) {
                        order.setStatus(EPaymentStatus.PAID);

                        de.saarland.events.model.Event event = order.getEvent();
                        event.setPremium(true);
                        event.setPremiumUntil(ZonedDateTime.now().plusDays(order.getPromotionDays()));
                        eventRepository.save(event);
                        paymentOrderRepository.save(order);

                        emailService.sendPromotionConfirmationEmail(order.getUser(), event);
                    }
                }
            }
        }
    }
}

