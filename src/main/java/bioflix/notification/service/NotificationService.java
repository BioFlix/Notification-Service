package bioflix.notification.service;

import bioflix.notification.dto.NotificationResponse;
import bioflix.notification.entity.NotificationChannel;
import bioflix.notification.entity.NotificationLog;
import bioflix.notification.entity.NotificationStatus;
import bioflix.notification.entity.UserContact;
import bioflix.notification.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Sends a notification to a user and writes the result to notification_log.
 * Failures are logged, not re-thrown – a broken mail server should not make RabbitMQ
 * redeliver the event forever.
 *
 * TODO (RabbitMQ): add listeners for user.*, booking.*, payment.* and ticket.issued that call
 * UserContactService.upsert/delete and notifyUser(...) with the right subject/body (see MessageFormatter).
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final UserContactService userContactService;
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final NotificationLogRepository logRepository;
    private final boolean smsEnabled;

    public NotificationService(UserContactService userContactService, EmailSender emailSender, SmsSender smsSender,
                               NotificationLogRepository logRepository,
                               @Value("${app.sms.enabled:false}") boolean smsEnabled) {
        this.userContactService = userContactService;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.logRepository = logRepository;
        this.smsEnabled = smsEnabled;
    }

    public void notifyUser(Long userId, String eventType, String subject, String body) {
        Optional<UserContact> contact = userContactService.find(userId);

        String email = contact.map(UserContact::getEmail).orElse(null);
        sendEmail(userId, eventType, email, subject, body);

        if (smsEnabled) {
            contact.map(UserContact::getPhone).ifPresent(phone -> sendSms(userId, eventType, phone, subject));
        }
    }

    /** Used for user.registered, where we already have the e-mail in the event. */
    public void sendEmail(Long userId, String eventType, String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.warn("No e-mail known for user {} – skipping {}", userId, eventType);
            save(userId, NotificationChannel.EMAIL, eventType, null, subject, body, NotificationStatus.SKIPPED, "No e-mail address");
            return;
        }
        try {
            emailSender.send(to, subject, body);
            save(userId, NotificationChannel.EMAIL, eventType, to, subject, body, NotificationStatus.SENT, null);
        } catch (MailException ex) {
            log.error("Could not send {} e-mail to {}", eventType, to, ex);
            save(userId, NotificationChannel.EMAIL, eventType, to, subject, body, NotificationStatus.FAILED, ex.getMessage());
        }
    }

    private void sendSms(Long userId, String eventType, String phone, String text) {
        smsSender.send(phone, text);
        save(userId, NotificationChannel.SMS, eventType, phone, null, text, NotificationStatus.SENT, null);
    }

    private void save(Long userId, NotificationChannel channel, String eventType, String recipient,
                      String subject, String body, NotificationStatus status, String error) {
        logRepository.save(new NotificationLog(userId, channel, eventType, recipient, subject, body, status, error));
    }

    public List<NotificationResponse> getForUser(Long userId) {
        return logRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(NotificationResponse::from).toList();
    }

    public List<NotificationResponse> getLatest() {
        return logRepository.findTop100ByOrderByCreatedAtDesc().stream().map(NotificationResponse::from).toList();
    }
}
