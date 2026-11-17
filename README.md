# notification-service

Sender e-mails (og senere SMS) når der sker noget i de andre services. Den ejer templates, afsendelsesforsøg,
retries og en log over hvad der er sendt – Booking, Payment og Ticket ved intet om e-mail.

Port **8086** · PostgreSQL `notifications_db` · RabbitMQ (consumer) · SMTP (Mailpit lokalt).
Datamodellen følger `cinema-data-model.html` (afsnittet *notifications database*).

## Kør lokalt

```bash
docker compose up --build
```

| Hvad | URL |
|---|---|
| API | http://localhost:8086/api/notifications/me (header `X-User-Id: <uuid>`) |
| Mailpit – indbakken | http://localhost:8025 |
| RabbitMQ UI | http://localhost:15672 (guest / guest) |
| Postgres | `localhost:5434`, db `notifications_db` (postgres / postgres) |

Er en port optaget, kan den ændres: `MAILPIT_UI_PORT=8026 MAILPIT_SMTP_PORT=1026 docker compose up --build`
(se `docker-compose.yml` for alle variabler). `SMS_ENABLED=true docker compose up` slår (log-)SMS til.

Køres servicen fra IntelliJ i stedet, skal Postgres, RabbitMQ og Mailpit køre på `localhost` (standardporte),
eller sæt `DB_HOST`, `DB_PORT`, `RABBITMQ_HOST`, `MAIL_HOST` osv.

### Prøv det af uden de andre services
Åbn RabbitMQ UI → *Exchanges* → `cinema.events` → *Publish message*:
- Routing key: `user.registered`
- Properties: `content_type = application/json`
- Payload (nyt `eventId` hver gang):
  ```json
  {"eventId":"2f0c7b9e-0f4e-4a2b-9a51-1c2d3e4f5a6b","type":"user.registered","version":1,"occurredAt":"2026-11-17T10:00:00Z",
   "producer":"manual","data":{"userId":"a91e44f0-1111-4c55-9a43-5d1b7f0e9a21","email":"anna@example.com","fullName":"Anna Hansen"}}
  ```
Velkomstmailen dukker op i Mailpit, og `GET /api/notifications/me` med `X-User-Id: a91e44f0-1111-4c55-9a43-5d1b7f0e9a21`
viser den som `SENT`. Kører user-service mod samme RabbitMQ, kommer `user.*`-events automatisk.

## Arkitektur

```
cinema.events (topic) ──[user.*, booking.confirmed, booking.cancelled, payment.*, ticket.issued]──► notification-service.events
                                                                                                         │ efter retries
cinema.events.dlx (direct) ◄─────────────────────────────────────────────────────────────────────────────┘
      └──► notification-service.events.dlq
```

`RabbitConfig` deklarerer exchange, kø, bindings og dead-letter-kø ved opstart. Deklarationer i RabbitMQ er
idempotente, så det er lige meget om producer eller consumer starter først.

### Tabeller
| Tabel | Indhold |
|---|---|
| `recipients` | Lokal kopi af brugerens kontaktoplysninger (`email`, `full_name`, `phone`, `sms_opt_in`, `deleted`), bygget fra `user.*`-events |
| `notifications` | Sendeloggen – én række pr. e-mail/SMS: `template`, `channel`, `status` (`QUEUED`/`SENT`/`FAILED`), `attempts`, `provider_message_id`, `last_error`, `trigger_event_id`/`trigger_event_type` |
| `processed_events` | Inbox: alle `eventId`’er der er behandlet færdigt |

Afvigelser fra HTML-modellen: status/kanal/template er `varchar` + check i stedet for Postgres-enums (lettere at
udvide), `recipients` har `last_event_at` + `version` (rækkefølge og samtidighed, se nedenfor), og der er
**ingen FK** fra `notifications.user_id` til `recipients` – et event kan have e-mailen med og komme før `user.registered`.

### Flow for et event
1. `NotificationEventListener` modtager beskeden og `EventParser` læser envelope’en (`version` > 1 → dead-letter).
2. `NotificationEventHandler` starter en transaktion og indsætter `eventId` i `processed_events`
   (`INSERT … ON CONFLICT DO NOTHING`). Findes den allerede, er eventet en dublet → kvitteres og springes over.
3. Ud fra `type`:
   - `user.*` → opdaterer `recipients`. `user.registered` sender også en velkomstmail.
   - Øvrige → finder modtagerens e-mail, renderer en template (`NotificationTemplates`) og sender.
     SMS kun hvis `SMS_ENABLED` **og** brugeren har `sms_opt_in` og et telefonnummer.
4. `NotificationService` finder eller opretter rækken i `notifications` (`QUEUED`), sender og sætter `SENT` +
   `provider_message_id`. Fejler afsendelsen, gemmes `attempts` + `last_error`, og beskeden prøves igen.
   Disse rækker skrives i deres egen transaktion, så loggen overlever selvom eventet rulles tilbage.

### Fejlhåndtering
| Situation | Hvad sker der |
|---|---|
| Ugyldig besked (ikke JSON, mangler `eventId`/`userId`, ukendt `version`) | Afvises straks → dead-letter-kø. Ingen retries. |
| Mailserveren er nede | Retry med backoff (2s, 4s, 8s, 16s). Derefter `FAILED` + dead-letter-kø. |
| E-mail ukendt (fx `booking.confirmed` før `user.registered`) | Samme retry. Events kan komme i forkert rækkefølge. |
| Brugeren er slettet | Ingen e-mail. Eventet kvitteres. |
| Samme event leveres to gange – også samtidigt | `processed_events` → anden kopi venter på den første og springes over. |

Retries venter i consumer-tråden, derfor kører der 3–10 consumers (`application.yml`), så ét event der
prøves igen ikke blokerer de andre. Hver consumer bruger to DB-forbindelser (inbox + sendelog), derfor pool på 25.

### Mønstre
- **Idempotent consumer / inbox:** `processed_events` + unik `one_send_per_event` på `(trigger_event_id, channel, template)`.
- **Lokal read-model:** `recipients` opdateres fra `user.*`-events, så servicen aldrig kalder user-service synkront.
- **Tombstone:** `user.deleted` sletter persondata men beholder rækken med `deleted = true`, så et sent ankommet
  `user.updated` ikke genopliver brugeren.
- **Kommutative handlers:** `last_event_at` – et `user.*`-event ældre end det seneste vi har anvendt ignoreres.
  Optimistisk låsning (`version`) forhindrer at to samtidige events overskriver hinanden.
- **SMTP bag et interface:** `EmailSender` / `SmtpEmailSender` – Mailpit kan skiftes til Postmark/SES uden ændringer i logikken.
- **QR-koder:** `ticket.issued` indeholder et signeret `qrToken`; `QrCodeGenerator` (ZXing) laver PNG’en der vedhæftes.

## REST-endpoints
Gatewayen validerer tokenet (via user-service) og sender `X-User-Id` (UUID) og `X-User-Role` videre.

| Metode | Sti | Adgang |
|---|---|---|
| GET | `/api/notifications/me` | Den kaldende bruger (`X-User-Id`) |
| GET | `/api/notifications/{id}` | Ejeren eller `ADMIN` |
| GET | `/api/notifications?status=FAILED` | `ADMIN` – seneste 100, evt. filtreret på status (`QUEUED`/`SENT`/`FAILED`) |
| GET | `/api/notifications/users/{userId}` | `ADMIN` |

## Event-kontrakt
Som i `cinema-data-model.html` (del 3). Alle events på `cinema.events` har samme envelope. Routing key = `type`.
Ukendte felter ignoreres (nye felter må kun tilføjes, ikke fjernes eller omdøbes).

```json
{
  "eventId": "6b1f0c1e-2d0a-4c55-9a43-5d1b7f0e9a21",
  "type": "booking.confirmed",
  "version": 1,
  "occurredAt": "2026-11-11T18:42:10Z",
  "producer": "booking-service",
  "correlationId": "d3c0…",
  "data": { }
}
```
`eventId` er påkrævet (UUID). `userId` er user-service’ UUID. Beløb er i øre (`13500` = 135,00 DKK), tider i UTC.

| Event | data (fra HTML-modellen) | Valgfrie ekstra felter vi bruger |
|---|---|---|
| `user.registered` / `user.updated` | `userId`, `email`, `fullName`, `phone`, `smsOptIn` | |
| `user.deleted` | `userId` | |
| `booking.confirmed` | `bookingId`, `userId`, `showtimeId`, `movieTitle`, `hallName`, `startsAt`, `seats[{row, number, type, price}]` | `cinemaName`, `totalAmount`, `currency`, `userEmail` |
| `booking.cancelled` | `bookingId`, `userId`, `reason`, `wasPaid` | `movieTitle`, `startsAt`, `userEmail` |
| `payment.completed` | `paymentId`, `bookingId`, `userId`, `amount`, `currency`, `receiptNumber`, `receiptUrl` | `userEmail` |
| `payment.failed` | `paymentId`, `bookingId`, `userId`, `reason` | `userEmail` |
| `payment.refunded` | `paymentId`, `refundId`, `bookingId`, `userId`, `amount`, `currency` | `userEmail` |
| `ticket.issued` | `bookingId`, `userId`, `tickets[{ticketNumber, row, number, qrToken}]` | `movieTitle`, `cinemaName`, `hallName`, `startsAt`, `userEmail` |

Mangler `totalAmount`, beregnes prisen som summen af `seats[].price`. Mangler de ekstra felter, udelades linjerne
i mailen. Forslag til Booking/Ticket: send `movieTitle`/`startsAt` med i `booking.cancelled` og `ticket.issued`,
så mailene bliver mere informative.

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

> Skemaet oprettes med `ddl-auto: update` + `schema.sql` (partielle indekser). De gamle tabeller
> `notification_log` og `user_contacts` bruges ikke længere og kan droppes.
