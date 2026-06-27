package pl.przychodnia.appointment.service;

import pl.przychodnia.appointment.domain.AppointmentType;

import java.util.UUID;

/**
 * Polecenie rezerwacji wizyty (FR-APPT-001).
 *
 * @param holderId użytkownik trzymający blokadę slotu (klient lub recepcja)
 * @param customerId właściciel pacjenta (dla rezerwacji przez recepcję != holderId)
 */
public record BookAppointmentCommand(UUID slotId, UUID holderId, UUID customerId,
                                     UUID patientId, UUID veterinarianId,
                                     AppointmentType type, String room) {
}
