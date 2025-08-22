package de.saarland.events.controller;

import de.saarland.events.dto.*;
import de.saarland.events.mapper.EventMapper;
import de.saarland.events.model.EStatus;
import de.saarland.events.model.Event;
import de.saarland.events.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/events")
public class AdminEventController {

    private final EventService eventService;
    private final EventMapper eventMapper;

    public AdminEventController(EventService eventService, EventMapper eventMapper) {
        this.eventService = eventService;
        this.eventMapper = eventMapper;
    }

    @GetMapping("/by-city/{cityName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<EventResponseDto>> getAdminEventsByCity(@PathVariable String cityName, Pageable pageable) {
        Page<Event> eventsPage = eventService.findAllAdminEventsByCity(cityName, pageable);
        Page<EventResponseDto> dtoPage = eventsPage.map(eventMapper::toResponseDto);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<EventResponseDto>> getAllEventsForAdmin(Pageable pageable) {
        Page<Event> eventsPage = eventService.findAllEventsForAdmin(pageable);
        return ResponseEntity.ok(eventsPage.map(eventMapper::toResponseDto));
    }

    @GetMapping("/by-city-count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CityEventCountDto>> getCityEventCounts() {
        return ResponseEntity.ok(eventService.getCityEventCounts());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponseDto> createEvent(@Valid @RequestBody EventRequestDto eventRequestDto) {
        Event eventToCreate = eventMapper.toEntity(eventRequestDto);
        Event createdEvent = eventService.createEvent(eventToCreate, eventRequestDto.getCategoryId(), eventRequestDto.getCityId());
        EventResponseDto responseDto = eventMapper.toResponseDto(createdEvent);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponseDto> updateEvent(@PathVariable Long id, @Valid @RequestBody EventUpdateDto eventUpdateDto) {
        Event eventData = eventMapper.toEntity(eventUpdateDto);
        Event updatedEvent = eventService.updateEvent(id, eventData, eventUpdateDto.getCategoryId(), eventUpdateDto.getCityId());
        EventResponseDto responseDto = eventMapper.toResponseDto(updatedEvent);
        return ResponseEntity.ok(responseDto);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponseDto> updateEventStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        EStatus newStatus = EStatus.valueOf(request.getStatus().toUpperCase());
        Event updatedEvent = eventService.updateEventStatus(id, newStatus);
        return ResponseEntity.ok(eventMapper.toResponseDto(updatedEvent));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminStatsDto> getStatistics() {
        AdminStatsDto stats = eventService.getAdminStatistics();
        return ResponseEntity.ok(stats);
    }
}