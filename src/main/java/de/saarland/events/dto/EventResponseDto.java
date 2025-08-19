// src/main/java/de/saarland/events/dto/EventResponseDto.java
package de.saarland.events.dto;

import de.saarland.events.model.EStatus;
import java.time.ZonedDateTime; // ИЗМЕНЕНО
import java.util.List;

public class EventResponseDto {
    private Long id;
    private ZonedDateTime eventDate; // ИЗМЕНЕНО
    private CityDto city;
    private String imageUrl;
    private CategoryDto category;
    private List<TranslationDto> translations;
    private EStatus status;

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ZonedDateTime getEventDate() { return eventDate; } // ИЗМЕНЕНО
    public void setEventDate(ZonedDateTime eventDate) { this.eventDate = eventDate; } // ИЗМЕНЕНО
    public CityDto getCity() { return city; }
    public void setCity(CityDto city) { this.city = city; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public CategoryDto getCategory() { return category; }
    public void setCategory(CategoryDto category) { this.category = category; }
    public List<TranslationDto> getTranslations() { return translations; }
    public void setTranslations(List<TranslationDto> translations) { this.translations = translations; }
    public EStatus getStatus() { return status; }
    public void setStatus(EStatus status) { this.status = status; }
}