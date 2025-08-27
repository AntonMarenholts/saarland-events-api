package de.saarland.events.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CityDto {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
}