package pl.przychodnia.appointment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.przychodnia.appointment.config.AppointmentProperties;
import pl.przychodnia.appointment.domain.Appointment;
import pl.przychodnia.appointment.domain.AppointmentSlot;
import pl.przychodnia.appointment.domain.exception.ResourceNotFoundException;
import pl.przychodnia.appointment.event.AppointmentEventPublisher;
import pl.przychodnia.appointment.event.AppointmentEvents;
import pl.przychodnia.appointment.repository.AppointmentRepository;
import pl.przychodnia.appointment.repository.AppointmentSlotRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {

    private final AppointmentRepository appointments;
    private final AppointmentSlotRepository slots;
    private final AppointmentEventPublisher publisher;
    private final AppointmentProperties properties;
    private final Clock clock;

    public AppointmentService(AppointmentRepository appointments,
                              AppointmentSlotRepository slots,
                              AppointmentEventPublisher publisher,
                              AppointmentProperties properties,
                              Clock clock) {
        this.appointments = appointments;
        this.slots = slots;
        this.publisher = publisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AppointmentSlot> availableSlots(UUID veterinarianId, Instant from, Instant to) {
        return slots.findByVeterinarianIdAndAvailableTrueAndStartTimeBetween(veterinarianId, from, to);
    }

    /**
     * Tymczasowa blokada slotu na czas {@code hold-duration} (FR-APPT-003).
     * Pesymistyczna blokada wiersza serializuje wyścig o ten sam termin.
     */
    @Transactional
    public SlotHold lockSlot(UUID slotId, UUID holderId) {
        Instant now = clock.instant();
        AppointmentSlot slot = lockedSlot(slotId);
        slot.lock(holderId, now.plus(properties.slot().holdDuration()), now);
        return new SlotHold(slot.getId(), holderId, slot.getLockedUntil());
    }

    /**
     * Potwierdzenie rezerwacji i utworzenie wizyty (FR-APPT-001).
     * Wymaga ważnej blokady slotu należącej do {@code holderId}.
     */
    @Transactional
    public Appointment book(BookAppointmentCommand cmd) {
        Instant now = clock.instant();
        AppointmentSlot slot = lockedSlot(cmd.slotId());
        slot.confirmBooking(cmd.holderId(), now);

        Appointment appointment = Appointment.confirm(
                cmd.customerId(), cmd.patientId(), cmd.veterinarianId(), slot, cmd.type(), cmd.room());
        appointments.save(appointment);

        publisher.publish(AppointmentEvents.Created.of(appointment));
        return appointment;
    }

    /** Anulowanie wizyty i zwolnienie slotu (FR-APPT-002). */
    @Transactional
    public Appointment cancel(UUID appointmentId) {
        Appointment appointment = appointments.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        appointment.cancel();
        slots.findByIdForUpdate(appointment.getSlotId()).ifPresent(AppointmentSlot::reopen);

        publisher.publish(AppointmentEvents.Cancelled.of(appointment));
        return appointment;
    }

    /**
     * Przesunięcie wizyty na inny, wcześniej zablokowany slot (FR-APPT-002).
     * Stary slot wraca do puli wolnych.
     */
    @Transactional
    public Appointment reschedule(UUID appointmentId, UUID newSlotId, UUID holderId) {
        Instant now = clock.instant();
        Appointment appointment = appointments.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        Instant previousStart = appointment.getStartTime();
        UUID previousSlotId = appointment.getSlotId();

        AppointmentSlot newSlot = lockedSlot(newSlotId);
        newSlot.confirmBooking(holderId, now);
        appointment.rescheduleTo(newSlot);

        if (!previousSlotId.equals(newSlotId)) {
            slots.findByIdForUpdate(previousSlotId).ifPresent(AppointmentSlot::reopen);
        }

        publisher.publish(AppointmentEvents.Rescheduled.of(appointment, previousStart));
        return appointment;
    }

    /** Cykliczne zwalnianie wygasłych blokad (uzupełnia FR-APPT-003). */
    @Transactional
    public int releaseExpiredHolds() {
        List<AppointmentSlot> expired = slots.findExpiredHolds(clock.instant());
        expired.forEach(AppointmentSlot::releaseHold);
        return expired.size();
    }

    private AppointmentSlot lockedSlot(UUID slotId) {
        return slots.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot", slotId));
    }
}
