package de.saarland.events.service;

import de.saarland.events.model.Reminder;
import de.saarland.events.repository.ReminderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReminderTaskService {

    private final ReminderRepository reminderRepository;
    private final EmailService emailService;

    public ReminderTaskService(ReminderRepository reminderRepository, EmailService emailService) {
        this.reminderRepository = reminderRepository;
        this.emailService = emailService;
    }


    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void processReminders() {
        System.out.println("Checking reminders... " + LocalDateTime.now());


        List<Reminder> dueReminders = reminderRepository.findAllByRemindAtBeforeAndIsSentFalse(LocalDateTime.now());

        if (dueReminders.isEmpty()) {
            return;
        }

        System.out.printf("Found %d reminders to send.\n", dueReminders.size());


        for (Reminder reminder : dueReminders) {
            emailService.sendReminderEmail(reminder.getUser(), reminder.getEvent());

            reminder.setSent(true);
            reminderRepository.save(reminder);
        }
    }
}