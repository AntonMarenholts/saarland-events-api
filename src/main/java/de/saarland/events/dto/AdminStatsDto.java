package de.saarland.events.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AdminStatsDto {

    private long totalEvents;
    private long pendingEvents;
    private long approvedEvents;
    private long totalUsers;
    private long totalCategories;

}