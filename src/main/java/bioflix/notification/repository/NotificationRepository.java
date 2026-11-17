package bioflix.notification.repository;

import bioflix.notification.entity.Notification;
import bioflix.notification.entity.NotificationChannel;
import bioflix.notification.entity.NotificationStatus;
import bioflix.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByTriggerEventIdAndChannelAndTemplate(UUID triggerEventId, NotificationChannel channel,
                                                                     NotificationType template);

    List<Notification> findByTriggerEventIdAndStatus(UUID triggerEventId, NotificationStatus status);

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Notification> findTop100ByOrderByCreatedAtDesc();

    List<Notification> findTop100ByStatusOrderByCreatedAtDesc(NotificationStatus status);
}
