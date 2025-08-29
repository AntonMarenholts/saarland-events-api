package de.saarland.events.service;

import de.saarland.events.model.Event;
import de.saarland.events.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
public class PremiumCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(PremiumCleanupService.class);
    private final EventRepository eventRepository;

    public PremiumCleanupService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Scheduled(cron = "0 0 1 * * ?") // Run every day at 1 AM
    @Transactional
    public void cleanupExpiredPremiums() {
        logger.info("Running scheduled job to clean up expired premium events...");

        List<Event> expiredEvents = eventRepository.findByIsPremiumTrueAndPremiumUntilBefore(ZonedDateTime.now());

        if (expiredEvents.isEmpty()) {
            logger.info("No expired premium events found.");
            return;
        }

        logger.info("Found {} expired premium events to clean up.", expiredEvents.size());
        for (Event event : expiredEvents) {
            event.setPremium(false);
            event.setPremiumUntil(null);
            eventRepository.save(event);
        }
    }
}
