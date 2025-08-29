package de.saarland.events.dto.payment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePaymentRequest {
    private Long eventId;
    private int days;
    private Long userId;
}
