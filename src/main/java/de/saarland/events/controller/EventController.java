package de.saarland.events.controller;

import de.saarland.events.dto.EventResponseDto;
import de.saarland.events.mapper.EventMapper;
import de.saarland.events.model.Event;
import de.saarland.events.service.EventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final EventMapper eventMapper;

    public EventController(EventService eventService, EventMapper eventMapper) {
        this.eventService = eventService;
        this.eventMapper = eventMapper;
    }

    @GetMapping
    public ResponseEntity<Page<EventResponseDto>> getAllEvents(
            @RequestParam Optional<String> city,
            @RequestParam Optional<Long> category,
            @RequestParam Optional<Integer> year,
            @RequestParam Optional<Integer> month,
            @RequestParam Optional<String> categoryName,
            @RequestParam Optional<String> keyword,
            Pageable pageable
    ) {
        Page<Event> eventsPage = eventService.findEvents(city, category, year, month, categoryName, keyword, pageable);
        Page<EventResponseDto> dtoPage = eventsPage.map(eventMapper::toResponseDto);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponseDto> getEventById(@PathVariable Long id) {
        EventResponseDto eventDto = eventMapper.toResponseDto(eventService.getEventById(id));
        return ResponseEntity.ok(eventDto);
    }
}