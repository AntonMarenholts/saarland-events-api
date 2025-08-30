package de.saarland.events.repository;

import de.saarland.events.model.EPaymentStatus;
import de.saarland.events.model.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByStripeSessionId(String stripeSessionId);

    Optional<PaymentOrder> findByEventIdAndStatus(Long eventId, EPaymentStatus status);

    void deleteAllByEventId(Long eventId);
}
