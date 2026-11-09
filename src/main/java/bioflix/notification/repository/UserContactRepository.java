package bioflix.notification.repository;

import bioflix.notification.entity.UserContact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserContactRepository extends JpaRepository<UserContact, Long> {
}
