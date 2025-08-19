// src/main/java/de/saarland/events/model/Event.java
package de.saarland.events.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;

import java.time.ZonedDateTime; // ИЗМЕНЕНО
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private ZonedDateTime eventDate; // ИЗМЕНЕНО

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    private String imageUrl;


    @JsonManagedReference
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Translation> translations = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EStatus status;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Reminder> reminders = new ArrayList<>();

    public Event() {}

    // Геттеры и сеттеры
    public List<Reminder> getReminders() { return reminders; }
    public void setReminders(List<Reminder> reminders) { this.reminders = reminders; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ZonedDateTime getEventDate() { return eventDate; } // ИЗМЕНЕНО
    public void setEventDate(ZonedDateTime eventDate) { this.eventDate = eventDate; } // ИЗМЕНЕНО
    public City getCity() { return city; }
    public void setCity(City city) { this.city = city; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public List<Translation> getTranslations() { return translations; }
    public void setTranslations(List<Translation> translations) { this.translations = translations; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public EStatus getStatus() { return status; }
    public void setStatus(EStatus status) { this.status = status; }
}