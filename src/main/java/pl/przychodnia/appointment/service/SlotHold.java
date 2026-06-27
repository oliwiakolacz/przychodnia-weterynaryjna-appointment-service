package pl.przychodnia.appointment.service;

import java.time.Instant;
import java.util.UUID;

/** Wynik tymczasowej blokady slotu (FR-APPT-003). */
public record SlotHold(UUID slotId, UUID holderId, Instant lockedUntil) {
}
