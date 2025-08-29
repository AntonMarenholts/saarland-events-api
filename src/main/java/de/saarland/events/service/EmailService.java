package de.saarland.events.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import de.saarland.events.model.Event;
import de.saarland.events.model.User;
import de.saarland.events.model.Translation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final SendGrid sendGrid;

    @Value("${app.email.from}")
    private String fromEmail;


    @Value("${app.frontend.url}")
    private String frontendUrl;

    public EmailService(@Value("${SENDGRID_API_KEY}") String sendGridApiKey) {
        this.sendGrid = new SendGrid(sendGridApiKey);
    }

    public void sendReminderEmail(User user, Event event) {
        String eventName = event.getTranslations().getFirst().getName();
        String subject = "Event Reminder: " + eventName;
        String textContent = String.format(
                "Hello, %s!\n\nWe remind you that the event you saved will start soon: '%s'.\nIt will happen %s.\n\n" +
                        "Best wishes, the Afisha Saarland team!",
                user.getUsername(),
                eventName,
                event.getEventDate().toString()
        );
        sendEmail(user.getEmail(), subject, textContent);
    }



    public void sendPasswordResetEmail(User user, String resetLink) {
        String subject = "Password Reset Request";
        String textContent = String.format(
                "Hello, %s!\n\nYou requested a password reset. Please click the link below to set a new password:\n%s\n\n" +
                        "If you did not request this, please ignore this email.\n\n" +
                        "Best wishes, the Afisha Saarland team!",
                user.getUsername(),
                resetLink
        );
        sendEmail(user.getEmail(), subject, textContent);
    }


    public void sendPromotionConfirmationEmail(User user, Event event) {
        String eventName = event.getTranslations().getFirst().getName();
        String subject = "Your event '" + eventName + "' has been promoted!";
        String textContent = String.format(
                "Hello, %s!\n\nThank you for your payment. Your event '%s' will now be featured in the premium block until %s.\n\n" +
                        "Best wishes, the Afisha Saarland team!",
                user.getUsername(),
                eventName,
                event.getPremiumUntil().toLocalDate().toString()
        );
        sendEmail(user.getEmail(), subject, textContent);
    }


    public void sendEventApprovedEmail(User user, Event event) {
        String eventName = event.getTranslations().stream()
                .filter(t -> "de".equals(t.getLocale()))
                .findFirst()
                .map(Translation::getName)
                .orElse(event.getTranslations().isEmpty() ? "Ihr Event" : event.getTranslations().get(0).getName());

        String subject = "Ihr Event '" + eventName + "' wurde genehmigt!";
        String promotionLink = frontendUrl + "/promote/" + event.getId();

        String textContent = String.format(
                "Hallo, %s!\n\nIhr Event '%s' wurde genehmigt und ist jetzt auf unserer Webseite veröffentlicht.\n\n" +
                        "Möchten Sie mehr Besucher erreichen? Bewerben Sie Ihr Event hier: %s\n\n" +
                        "Mit freundlichen Grüßen,\nIhr Team von Afisha Saarland!",
                user.getUsername(),
                eventName,
                promotionLink
        );
        sendEmail(user.getEmail(), subject, textContent);
    }


    private void sendEmail(String toEmail, String subject, String textContent) {
        Email from = new Email(fromEmail);
        Email to = new Email(toEmail);
        Content content = new Content("text/plain", textContent);
        Mail mail = new Mail(from, subject, to, content);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);
            logger.info("Email sent to {}. Subject: '{}'. Status code: {}", toEmail, subject, response.getStatusCode());
        } catch (IOException ex) {
            logger.error("Error sending email to {}: {}", toEmail, ex.getMessage());
            throw new RuntimeException("Failed to send email", ex);
        }
    }
}

