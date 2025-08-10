package de.saarland.events.model;

import com.fasterxml.jackson.annotation.JsonBackReference; // <-- ДОБАВИТЬ ЭТОТ ИМПОРТ
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reminders")
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-reminder") // <-- ДОБАВИТЬ ЭТУ АННОТАЦИЮ
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @JsonBackReference("event-reminder") // <-- ДОБАВИТЬ ЭТУ АННОТАЦИЮ
    private Event event;

    @Column(nullable = false)
    private LocalDateTime remindAt;

    @Column(nullable = false)
    private boolean isSent = false;

    // --- Конструкторы, геттеры и сеттеры ---
    public Reminder() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public LocalDateTime getRemindAt() { return remindAt; }
    public void setRemindAt(LocalDateTime remindAt) { this.remindAt = remindAt; }
    public boolean isSent() { return isSent; }
    public void setSent(boolean sent) { isSent = sent; }
}