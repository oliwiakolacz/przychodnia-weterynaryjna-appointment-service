# Appointment Service

Autorzy: Oliwia Kołacz, Marek Kubiński

Mikroserwis rezerwacji wizyt dla systemu przychodni weterynaryjnej.
Realizuje wymagania:
**FR-APPT-001** (rezerwacja wizyty), **FR-APPT-002** (anulowanie i przesunięcie wizyty)
oraz **FR-APPT-003** (tymczasowa blokada slotu wizyty na 5 min).

## Stos
Spring Boot 3.3 (Java 21) · Spring Web · Spring Data JPA · PostgreSQL · Spring Kafka · JUnit 5 / Mockito.

## Model i kluczowe decyzje
- Po utworzeniu/anulowaniu/przesunięciu wizyty publikowane jest zdarzenie na topic
  `appointment.events` (klucz = id wizyty), które jest konsumowane przez m.in. przez Notification Service.
- `AppointmentSlot` (statusy: wolny, tymczasowo zablokowany, zarezerwowany).
- Blokada wygasa po  domyślnie 5 min (`appointment.slot.hold-duration`) ; 
- `ExpiredHoldsCleaner` cyklicznie zwalnia wygasłe blokady.


## API
| Metoda | Ścieżka | Wymaganie                                  |
|---|---|--------------------------------------------|
| GET  | `/api/v1/slots?veterinarianId=&from=&to=` | lista wolnych slotów wizyt weterynaryjnych |
| POST | `/api/v1/slots/{slotId}/holds` | FR-APPT-003 — blokada wizyty ns 5 min      |
| POST | `/api/v1/appointments` | FR-APPT-001 — rezerwacja wizyty            |
| POST | `/api/v1/appointments/{id}/cancellation` | FR-APPT-002 — anulowanie wizyty            |
| POST | `/api/v1/appointments/{id}/reschedule` | FR-APPT-002 — przesunięcie wizyty          |

Przepływ rezerwacji: `GET sloty` → `POST hold` (5-min blokada slotu) → `POST appointments` (potwierdzenie).

Kody błędów: `404` nie znaleziono, `409` slot zajęty / blokada wygasła / zły stan wizyty,
`422` niepoprawna walidacja żądania.

## Uruchomienie
```bash
docker compose up -d          # PostgreSQL + Kafka
mvn spring-boot:run           # serwis na :8083
```

## Testy
```bash
mvn test
```
- `AppointmentSlotTest`, `AppointmentTest` — logika domenowa (czysty JUnit).
- `AppointmentServiceTest` — orkiestracja (Mockito, `Clock.fixed`).
- `SlotLockingConcurrencyIT` — wyścig o slot na realnym H2.
- `AppointmentControllerIT` — REST end-to-end (MockMvc).

## Uwagi produkcyjne
- `ddl-auto: update` jest tylko dla demo
