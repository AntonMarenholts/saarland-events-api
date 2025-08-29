package de.saarland.events.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
public class ReviewResponseDto {
    private Long id;
    private int rating;
    private String comment;
    private ZonedDateTime createdAt;
    private String username;
    private Long userId;
}