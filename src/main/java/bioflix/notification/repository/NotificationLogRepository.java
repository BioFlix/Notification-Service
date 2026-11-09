package bioflix.notification.repository;

import bioflix.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<NotificationLog> findTop100ByOrderByCreatedAtDesc();
}
