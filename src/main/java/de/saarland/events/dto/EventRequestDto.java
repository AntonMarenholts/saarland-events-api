package de.saarland.events.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
public class EventRequestDto {

    @NotNull(message = "Event date cannot be empty")
    @Future(message = "The event date must be in the future.")
    private ZonedDateTime eventDate;

    private ZonedDateTime endDate;

    @NotNull(message = "City ID cannot be empty")
    private Long cityId;

    private String imageUrl;

    @NotNull(message = "Category ID cannot be empty")
    private Long categoryId;

    @NotEmpty(message = "Translation list cannot be empty")
    @Size(min = 1, message = "There must be at least one translation (German)")
    private List<TranslationDto> translations;

    private String recaptchaToken;
}