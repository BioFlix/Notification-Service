# notification-service

Sender e-mails (og senere SMS) når der sker noget i de andre services. Den ejer templates, afsendelsesforsøg,
retries og en log over hvad der er sendt – Booking, Payment og Ticket ved intet om e-mail.

Port **8086** · PostgreSQL `notifications_db` · RabbitMQ (consumer) · SMTP (Mailpit lokalt).

## Kør lokalt

```bash
docker compose up --build
```

| Hvad | URL |
|---|---|
| API | http://localhost:8086/api/notifications/me (header `X-User-Id`) |
| Mailpit – indbakken | http://localhost:8025 |
| RabbitMQ UI | http://localhost:15672 (guest / guest) |
| Postgres | `localhost:5434`, db `notifications_db` (postgres / postgres) |

Er en port optaget, kan den ændres: `MAILPIT_UI_PORT=8026 MAILPIT_SMTP_PORT=1026 docker compose up --build`
(se `docker-compose.yml` for alle variabler).

Køres servicen fra IntelliJ i stedet, skal Postgres, RabbitMQ og Mailpit køre på `localhost` (standardporte),
eller sæt `DB_HOST`, `DB_PORT`, `RABBITMQ_HOST`, `MAIL_HOST` osv.

### Prøv det af uden de andre services
Åbn RabbitMQ UI → *Exchanges* → `cinema.events` → *Publish message*:
- Routing key: `user.registered`
- Properties: `content_type = application/json`
- Payload:
  ```json
  {"eventId":"2f0c7b9e-0f4e-4a2b-9a51-1c2d3e4f5a6b","type":"user.registered","occurredAt":"2026-11-11T10:00:00Z",
   "producer":"manual","data":{"userId":42,"email":"anna@example.com","fullName":"Anna Hansen"}}
  ```
Velkomstmailen dukker op i Mailpit, og `GET /api/notifications/me` med `X-User-Id: 42` viser den som `SENT`.

## Arkitektur

```
cinema.events (topic) ──[user.*, booking.confirmed, booking.cancelled, payment.*, ticket.issued]──► notification-service.events
                                                                                                         │ efter retries
cinema.events.dlx (direct) ◄─────────────────────────────────────────────────────────────────────────────┘
      └──► notification-service.events.dlq
```

`RabbitConfig` deklarerer exchange, kø, bindings og dead-letter-kø ved opstart. Deklarationer i RabbitMQ er
idempotente, så det er lige meget om producer eller consumer starter først.

### Flow for et event
1. `NotificationEventListener` modtager beskeden og `EventParser` læser envelope’en.
2. `NotificationEventHandler` vælger hvad der skal ske ud fra `type`:
   - `user.*` → opdaterer den lokale kopi af kontaktoplysninger (`user_contacts`). `user.registered` sender også en velkomstmail.
   - Øvrige → finder modtagerens e-mail, renderer en template (`NotificationTemplates`) og sender.
3. `NotificationService` finder eller opretter en række i `notification_log` (status `PENDING`), sender, og
   sætter `SENT`. Fejler afsendelsen, gemmes `attempts` + `last_error`, og beskeden prøves igen.

### Fejlhåndtering
| Situation | Hvad sker der |
|---|---|
| Ugyldig besked (ikke JSON, mangler `eventId`/`type`/`userId`) | Afvises straks → dead-letter-kø. Ingen retries. |
| Mailserveren er nede | Retry med backoff (2s, 4s, 8s, 16s). Derefter `FAILED` + dead-letter-kø. |
| E-mail ukendt (fx `booking.confirmed` før `user.registered`) | Samme retry. Events kan komme i forkert rækkefølge. |
| Brugeren er slettet | Ingen e-mail. Eventet kvitteres. |
| Samme event leveres to gange | Rækken findes allerede som `SENT` → intet sendes igen. |

Retries venter i consumer-tråden, derfor kører der 3–10 consumers (`application.yml`), så ét event der
prøves igen ikke blokerer de andre.

### Mønstre
- **Idempotent consumer:** unik constraint på `(event_id, type, channel)` i `notification_log`. RabbitMQ leverer
  at-least-once, så samme event kan komme flere gange uden at kunden får to mails.
- **Lokal read-model:** `user_contacts` opdateres fra `user.*`-events, så servicen aldrig kalder user-service synkront.
- **Tombstone:** `user.deleted` sletter persondata men beholder rækken med `deleted = true`, så et sent ankommet
  `user.updated` ikke genopliver brugeren.
- **Kommutative handlers:** `last_event_at` på kontakten – et `user.*`-event der er ældre end det seneste
  vi har anvendt, ignoreres. Optimistisk låsning (`@Version`) forhindrer at to samtidige events overskriver hinanden.
- **SMTP bag et interface:** `EmailSender` / `SmtpEmailSender` – Mailpit kan skiftes til Postmark/SES uden ændringer i logikken.

## REST-endpoints
Gatewayen validerer tokenet (via user-service) og sender `X-User-Id` og `X-User-Role` videre.

| Metode | Sti | Adgang |
|---|---|---|
| GET | `/api/notifications/me` | Den kaldende bruger (`X-User-Id`) |
| GET | `/api/notifications/{id}` | Ejeren eller `ADMIN` |
| GET | `/api/notifications?status=FAILED` | `ADMIN` – seneste 100, evt. filtreret på status |
| GET | `/api/notifications/users/{userId}` | `ADMIN` |

## Event-kontrakt
Alle events på `cinema.events` har samme envelope. Routing key = `type`. Ukendte felter ignoreres
(nye felter må kun tilføjes, ikke fjernes eller omdøbes).

```json
{
  "eventId": "6b1f0c1e-2d0a-4c55-9a43-5d1b7f0e9a21",
  "type": "booking.confirmed",
  "occurredAt": "2026-11-11T18:42:10Z",
  "producer": "booking-service",
  "correlationId": "b-1001",
  "data": { }
}
```
`eventId` er påkrævet (UUID) – det er den idempotens bygger på. `correlationId` (fx bookingId) logges, så
en hel booking kan spores på tværs af services. Beløb er i øre (`13500` = 135,00 DKK), tider i UTC (ISO-8601).

`data` pr. event (felter markeret * er påkrævet; `userEmail` er valgfri overalt – mangler den, bruges `user_contacts`):

| Event | data |
|---|---|
| `user.registered` / `user.updated` | `userId`*, `email`, `fullName`, `phone` |
| `user.deleted` | `userId`* |
| `booking.confirmed` | `bookingId`, `userId`*, `userEmail`, `movieTitle`, `cinemaName`, `hallName`, `startsAt`, `seats[{row, number}]`, `totalAmount`, `currency` |
| `booking.cancelled` | `bookingId`, `userId`*, `userEmail`, `reason` (`USER`/`PAYMENT_FAILED`/`EXPIRED`/`SHOWTIME_CANCELLED`), `movieTitle`, `startsAt` |
| `payment.completed` | `paymentId`, `bookingId`, `userId`*, `userEmail`, `amount`, `currency`, `receiptNumber` |
| `payment.failed` | `paymentId`, `bookingId`, `userId`*, `userEmail`, `reason` |
| `payment.refunded` | `paymentId`, `bookingId`, `userId`*, `userEmail`, `amount`, `currency` |
| `ticket.issued` | `bookingId`, `userId`*, `userEmail`, `movieTitle`, `cinemaName`, `hallName`, `startsAt`, `tickets[{ticketCode, row, number, qrPng}]` – `qrPng` er Base64 PNG og vedhæftes mailen |

Servicen publicerer ingen events.

## Konfiguration
| Variabel | Standard |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_USER` / `DB_PASSWORD` | `localhost` / `5432` / `postgres` / `postgres` |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | `localhost` / `5672` / `guest` / `guest` |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USER` / `MAIL_PASSWORD` | `localhost` / `1025` / – / – |
| `MAIL_FROM` | `no-reply@bioflix.local` |
| `SMS_ENABLED` | `false` – SMS (Twilio) er ikke koblet på endnu, `SmsSender` logger kun |

I produktion: peg `MAIL_*` på Postmark eller Amazon SES (begge har SMTP).

> Skemaet oprettes med `ddl-auto: update`. Har du en gammel `notification_log`-tabel med rækker fra
> start-koden, så drop tabellen én gang – de nye NOT NULL-kolonner kan ikke tilføjes til eksisterende rækker.
