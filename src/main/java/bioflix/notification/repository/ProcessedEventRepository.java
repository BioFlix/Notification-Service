package bioflix.notification.repository;

import bioflix.notification.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    /**
     * Claims the event. Returns 0 if it was already processed. If another consumer is processing the same
     * event right now, this waits until that transaction ends: 0 if it committed, 1 if it rolled back.
     */
    @Modifying
    @Query(value = """
            INSERT INTO processed_events (event_id, event_type, processed_at)
            VALUES (:eventId, :eventType, now())
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int claim(@Param("eventId") UUID eventId, @Param("eventType") String eventType);
}
