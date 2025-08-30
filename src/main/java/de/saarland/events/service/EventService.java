package de.saarland.events.service;

import de.saarland.events.dto.AdminStatsDto;
import de.saarland.events.dto.CityEventCountDto;
import de.saarland.events.model.*;
import de.saarland.events.repository.*;
import de.saarland.events.specification.EventSpecification;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import de.saarland.events.repository.PaymentOrderRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final CityRepository cityRepository;
    private final EventSpecification eventSpecification;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PaymentOrderRepository paymentOrderRepository;
    private final ReviewRepository reviewRepository;
    private final ReminderRepository reminderRepository;

    public EventService(EventRepository eventRepository, CategoryRepository categoryRepository, CityRepository cityRepository, EventSpecification eventSpecification, UserRepository userRepository, EmailService emailService, PaymentOrderRepository paymentOrderRepository, ReviewRepository reviewRepository, ReminderRepository reminderRepository) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.cityRepository = cityRepository;
        this.eventSpecification = eventSpecification;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.paymentOrderRepository = paymentOrderRepository;
        this.reviewRepository = reviewRepository;
        this.reminderRepository = reminderRepository;
    }

    @Transactional(readOnly = true)
    public Page<Event> findAllAdminEventsByCity(String cityName, Pageable pageable) {
        return eventRepository.findByCityNameAndStatusIn(
                cityName,
                Arrays.asList(EStatus.APPROVED, EStatus.REJECTED),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<Event> findEvents(Optional<String> city, Optional<Long> categoryId, Optional<Integer> year, Optional<Integer> month, Optional<String> categoryName, Optional<String> keyword, Pageable pageable) {
        Specification<Event> spec = eventSpecification.findByCriteria(city, categoryId, year, month, categoryName, keyword);
        return eventRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event with ID " + id + " not found"));
    }

    @Transactional
    public Event createEvent(Event event, Long categoryId, Long cityId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID " + userId));
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

        event.setCreatedBy(user);
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
        userRepository.saveAll(usersWithFavorite);

        paymentOrderRepository.deleteAllByEventId(id);
        reviewRepository.deleteAllByEventId(id);
        reminderRepository.deleteAllByEventId(id);

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
    public Event updateEventStatus(Long eventId, EStatus newStatus) {
        Event existingEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event with ID " + eventId + " not found"));

        EStatus oldStatus = existingEvent.getStatus();
        existingEvent.setStatus(newStatus);
        Event savedEvent = eventRepository.save(existingEvent);

        if (oldStatus == EStatus.PENDING && newStatus == EStatus.APPROVED) {
            User creator = savedEvent.getCreatedBy();
            if (creator != null) {
                emailService.sendEventApprovedEmail(creator, savedEvent);
            }
        }

        return savedEvent;
    }

    @Transactional(readOnly = true)
    public Page<Event> findAllEventsForAdmin(Pageable pageable) {
        return eventRepository.findByStatusOrderByEventDateAsc(EStatus.PENDING, pageable);
    }

    @Transactional(readOnly = true)
    public List<CityEventCountDto> getCityEventCounts() {
        return eventRepository.countEventsByCity();
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

    @Transactional(readOnly = true)
    public Page<Event> findEventsByCreator(Long userId, Pageable pageable) {
        return eventRepository.findByCreatedBy_Id(userId, pageable);
    }

    @Transactional
    public Event updateUserEvent(Long eventId, Event updatedEventData, Long categoryId, Long cityId, Long userId) {
        Event existingEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event with ID " + eventId + " not found"));

        if (!existingEvent.getCreatedBy().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("User does not have permission to update this event");
        }

        return updateEvent(eventId, updatedEventData, categoryId, cityId);
    }

    @Transactional
    public void deleteUserEvent(Long eventId, Long userId) {
        Event eventToDelete = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event with ID " + eventId + " not found."));

        if (!eventToDelete.getCreatedBy().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("User does not have permission to delete this event");
        }

        deleteEvent(eventId);
    }
}

