package bioflix.notification.repository;

import bioflix.notification.entity.NotificationChannel;
import bioflix.notification.entity.NotificationLog;
import bioflix.notification.entity.NotificationStatus;
import bioflix.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    Optional<NotificationLog> findByEventIdAndTypeAndChannel(UUID eventId, NotificationType type, NotificationChannel channel);

    List<NotificationLog> findByEventIdAndStatus(UUID eventId, NotificationStatus status);

    List<NotificationLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<NotificationLog> findTop100ByOrderByCreatedAtDesc();

    List<NotificationLog> findTop100ByStatusOrderByCreatedAtDesc(NotificationStatus status);
}
