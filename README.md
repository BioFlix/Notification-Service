# notification-service

Sender e-mails (og senere SMS) når der sker noget i de andre services. Port **8086**, PostgreSQL `notifications_db`.

Lokalt går mails til **Mailpit** fra docker-compose – se dem på http://localhost:8025.
I produktion: peg `MAIL_HOST/MAIL_PORT/MAIL_USER/MAIL_PASSWORD` på Postmark eller Amazon SES (begge har SMTP).

## Sådan finder den e-mailadressen
Den kalder **ikke** user-service. I stedet holder den sin egen tabel `user_contacts` opdateret ud fra
`user.registered/updated/deleted`. Alle forsøg logges i `notification_log` (SENT / FAILED / SKIPPED).

## Endpoints
| Metode | Sti |
|---|---|
| GET | `/api/notifications/me` (`X-User-Id`) |
| GET | `/api/notifications` – seneste 100, TODO: kun admin |

## Events (når RabbitMQ-repoet er klar)
Skal lytte på `user.*`, `booking.confirmed`, `booking.cancelled`, `payment.*` og `ticket.issued`. Publicerer intet.
Indtil da sendes der ingen mails – servicen har logikken (`NotificationService`, `UserContactService`) men intet der kalder den.
SMS (Twilio) er slået fra – se `SmsSender`.
