package de.saarland.events.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranslationDto {
    private String locale;
    private String name;
    private String description;
}