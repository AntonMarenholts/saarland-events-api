package de.saarland.events.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class EventRequestDto {

    @NotNull(message = "Event date cannot be empty")
    @Future(message = "The event date must be in the future.")
    private LocalDateTime eventDate;


    @NotNull(message = "City ID cannot be empty")
    private Long cityId;


    private String imageUrl;

    @NotNull(message = "Category ID cannot be empty")
    private Long categoryId;

    @NotEmpty(message = "Translation list cannot be empty")
    @Size(min = 1, message = "There must be at least one translation (German)")
    private List<TranslationDto> translations;


    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }
    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public List<TranslationDto> getTranslations() { return translations; }
    public void setTranslations(List<TranslationDto> translations) { this.translations = translations; }
}