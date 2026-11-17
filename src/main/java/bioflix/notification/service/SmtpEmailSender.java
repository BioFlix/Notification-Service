package bioflix.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.List;

/** SMTP implementation – Mailpit locally, Postmark / Amazon SES (both speak SMTP) in production. */
@Component
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailSender(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String body, List<RenderedMessage.Attachment> attachments) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            boolean multipart = attachments != null && !attachments.isEmpty();
            MimeMessageHelper helper = new MimeMessageHelper(message, multipart, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            if (multipart) {
                for (RenderedMessage.Attachment attachment : attachments) {
                    helper.addAttachment(attachment.filename(), new ByteArrayResource(attachment.content()),
                            attachment.contentType());
                }
            }
        } catch (MessagingException ex) {
            throw new MailPreparationException("Could not build e-mail to " + to, ex);
        }
        mailSender.send(message);
    }
}
