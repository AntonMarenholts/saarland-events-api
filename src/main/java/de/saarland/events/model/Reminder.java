// src/main/java/de/saarland/events/model/Reminder.java
package de.saarland.events.model;

import jakarta.persistence.*;
import java.time.ZonedDateTime; // ИЗМЕНЕНО

@Entity
@Table(name = "reminders")
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private ZonedDateTime remindAt; // ИЗМЕНЕНО

    @Column(nullable = false)
    private boolean isSent = false;

    public Reminder() {}

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public ZonedDateTime getRemindAt() { return remindAt; } // ИЗМЕНЕНО
    public void setRemindAt(ZonedDateTime remindAt) { this.remindAt = remindAt; } // ИЗМЕНЕНО
    public boolean isSent() { return isSent; }
    public void setSent(boolean sent) { isSent = sent; }
}