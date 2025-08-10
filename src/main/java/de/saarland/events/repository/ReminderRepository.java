package de.saarland.events.repository;

import de.saarland.events.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findAllByRemindAtBeforeAndIsSentFalse(LocalDateTime currentTime);
}