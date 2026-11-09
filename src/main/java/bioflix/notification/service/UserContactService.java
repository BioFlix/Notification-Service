package bioflix.notification.service;

import bioflix.notification.entity.UserContact;
import bioflix.notification.repository.UserContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Keeps the local user_contacts table in sync with user.* events. */
@Service
public class UserContactService {

    private final UserContactRepository repository;

    public UserContactService(UserContactRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserContact upsert(Long userId, String email, String fullName, String phone) {
        UserContact contact = repository.findById(userId).orElseGet(() -> new UserContact(userId));
        contact.update(email, fullName, phone);
        return repository.save(contact);
    }

    @Transactional
    public void delete(Long userId) {
        repository.deleteById(userId);
    }

    @Transactional(readOnly = true)
    public Optional<UserContact> find(Long userId) {
        return repository.findById(userId);
    }
}
