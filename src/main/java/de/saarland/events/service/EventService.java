// src/main/java/de/saarland/events/service/EventService.java
package de.saarland.events.service;

import de.saarland.events.dto.AdminStatsDto;
import de.saarland.events.model.*;
import de.saarland.events.repository.CategoryRepository;
import de.saarland.events.repository.CityRepository;
import de.saarland.events.repository.EventRepository;
import de.saarland.events.repository.UserRepository;
import de.saarland.events.specification.EventSpecification;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.time.LocalDate;
import java.util.Comparator; // ИМПОРТ
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventService {
    // ... (поля и конструктор без изменений) ...
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final CityRepository cityRepository;
    private final EventSpecification eventSpecification;
    private final UserRepository userRepository;


    public EventService(EventRepository eventRepository, CategoryRepository categoryRepository, CityRepository cityRepository, EventSpecification eventSpecification, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.cityRepository = cityRepository;
        this.eventSpecification = eventSpecification;
        this.userRepository = userRepository;
    }

    // ... (остальные методы) ...

    @Transactional(readOnly = true)
    public List<Event> findAllAdminEventsByCity(String cityName) {
        // V-- ЭТОТ МЕТОД ПОЛНОСТЬЮ ИЗМЕНЕН --V
        // 1. Получаем неотсортированный список из репозитория
        List<Event> events = eventRepository.findByCityNameAndStatusIn(
                cityName,
                Arrays.asList(EStatus.APPROVED, EStatus.REJECTED)
        );
        // 2. Сортируем его вручную в коде (от новых к старым)
        events.sort(Comparator.comparing(Event::getEventDate).reversed());
        // 3. Возвращаем отсортированный список
        return events;
        // ^-- КОНЕЦ ИЗМЕНЕНИЙ --^
    }

    // ... (остальные методы без изменений) ...
    @Transactional(readOnly = true)
    public List<Event> findEvents(Optional<String> city, Optional<Long> categoryId, Optional<Integer> year, Optional<Integer> month, Optional<String> categoryName, Optional<String> keyword) {
        Specification<Event> spec = eventSpecification.findByCriteria(city, categoryId, year, month, categoryName, keyword);
        return eventRepository.findAll(spec);
    }

    @Transactional(readOnly = true)
    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event with ID " + id + " not found"));
    }

    @Transactional
    public Event createEvent(Event event, Long categoryId, Long cityId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category with ID " + categoryId + " not found"));
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City with ID " + cityId + " not found"));


        String germanName = event.getTranslations().stream()
                .filter(t -> "de".equals(t.getLocale()))
                .findFirst()
                .map(Translation::getName)
                .orElse(null);

        if (germanName != null && !germanName.trim().isEmpty()) {
            LocalDate eventDate = event.getEventDate().toLocalDate();
            if (eventRepository.existsDuplicate(germanName, cityId, eventDate)) {

                throw new IllegalArgumentException(
                        "Ein Ereignis mit demselben Namen, in derselben Stadt und am selben Datum ist bereits vorhanden."
                );
            }
        }
        event.setCategory(category);
        event.setCity(city);
        event.getTranslations().forEach(translation -> translation.setEvent(event));

        if (event.getStatus() == null) {
            event.setStatus(EStatus.PENDING);
        }

        return eventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(Long id) {
        Event eventToDelete = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cannot delete. Event with ID " + id + " not found."));
        List<User> usersWithFavorite = userRepository.findByFavoriteEventsContains(eventToDelete);
        for (User user : usersWithFavorite) {
            user.getFavoriteEvents().remove(eventToDelete);
        }
        eventRepository.delete(eventToDelete);
    }

    @Transactional
    public Event updateEvent(Long eventId, Event updatedEventData, Long categoryId, Long cityId) {
        Event existingEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event with ID " + eventId + " not found"));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category with ID " + categoryId + " not found"));
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City with ID " + cityId + " not found"));

        existingEvent.setEventDate(updatedEventData.getEventDate());
        existingEvent.setCity(city);
        existingEvent.setImageUrl(updatedEventData.getImageUrl());
        existingEvent.setCategory(category);

        existingEvent.getTranslations().clear();
        updatedEventData.getTranslations().forEach(translation -> {
            translation.setEvent(existingEvent);
            existingEvent.getTranslations().add(translation);
        });

        return eventRepository.save(existingEvent);
    }

    @Transactional
    public Event updateEventStatus(Long eventId, EStatus status) {
        Event existingEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event with ID " + eventId + " not found"));
        existingEvent.setStatus(status);
        return eventRepository.save(existingEvent);
    }

    @Transactional(readOnly = true)
    public List<Event> findAllEventsForAdmin() {
        return eventRepository.findByStatusOrderByEventDateAsc(EStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public Map<String, List<Event>> getGroupedEventsByCity() {
        return eventRepository.findAll().stream()
                .filter(event -> event.getStatus() == EStatus.APPROVED || event.getStatus() == EStatus.REJECTED)
                .sorted((e1, e2) -> e2.getEventDate().compareTo(e1.getEventDate()))
                .collect(Collectors.groupingBy(event -> event.getCity().getName()));
    }

    @Transactional(readOnly = true)
    public AdminStatsDto getAdminStatistics() {
        long totalEvents = eventRepository.count();
        long pendingEvents = eventRepository.countByStatus(EStatus.PENDING);
        long approvedEvents = eventRepository.countByStatus(EStatus.APPROVED);
        long totalUsers = userRepository.count();
        long totalCategories = categoryRepository.count();

        return new AdminStatsDto(totalEvents, pendingEvents, approvedEvents, totalUsers, totalCategories);
    }
}