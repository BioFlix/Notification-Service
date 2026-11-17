package bioflix.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SMS is "not yet wired" in the diagram (Twilio – optional). Only used when app.sms.enabled=true.
 * For now we only log. To add Twilio: add com.twilio.sdk:twilio and call Message.creator(...) here.
 */
@Component
public class SmsSender {

    private static final Logger log = LoggerFactory.getLogger(SmsSender.class);

    /** @return the provider's message id – null until Twilio is wired */
    public String send(String phoneNumber, String text) {
        log.info("[SMS not wired] to {}: {}", phoneNumber, text);
        return null;
    }
}
