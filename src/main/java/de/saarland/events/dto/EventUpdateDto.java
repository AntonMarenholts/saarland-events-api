// src/main/java/de/saarland/events/dto/EventUpdateDto.java
package de.saarland.events.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.ZonedDateTime; // ИЗМЕНЕНО
import java.util.List;

public class EventUpdateDto {

    @NotNull(message = "Event date cannot be empty")
    private ZonedDateTime eventDate; // ИЗМЕНЕНО

    @NotNull(message = "City ID cannot be empty")
    private Long cityId;

    private String imageUrl;

    @NotNull(message = "Category ID cannot be empty")
    private Long categoryId;

    @NotEmpty(message = "Translation list cannot be empty")
    @Size(min = 1, message = "There must be at least one translation (German)")
    private List<TranslationDto> translations;

    // Геттеры и сеттеры
    public ZonedDateTime getEventDate() { return eventDate; } // ИЗМЕНЕНО
    public void setEventDate(ZonedDateTime eventDate) { this.eventDate = eventDate; } // ИЗМЕНЕНО
    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public List<TranslationDto> getTranslations() { return translations; }
    public void setTranslations(List<TranslationDto> translations) { this.translations = translations; }
}