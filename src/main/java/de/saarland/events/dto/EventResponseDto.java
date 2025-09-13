package de.saarland.events.dto;

import de.saarland.events.model.EStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
public class EventResponseDto {
    private Long id;
    private ZonedDateTime eventDate;
    private ZonedDateTime endDate;
    private CityDto city;
    private String imageUrl;
    private CategoryDto category;
    private List<TranslationDto> translations;
    private EStatus status;
    private Long createdByUserId;
    private boolean isPremium;
    private ZonedDateTime premiumUntil;
}