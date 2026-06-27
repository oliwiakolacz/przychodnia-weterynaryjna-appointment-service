package pl.przychodnia.appointment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import pl.przychodnia.appointment.domain.exception.IllegalAppointmentStateException;

import java.time.Instant;
import java.util.UUID;

/**
 * Wizyta. Tworzona w stanie CONFIRMED po potwierdzeniu rezerwacji (FR-APPT-001).
 * Anulowanie i przesunięcie realizują FR-APPT-002.
 */
@Entity
@Table(name = "appointment")
public class Appointment {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private UUID veterinarianId;

    @Column(nullable = false)
    private UUID slotId;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    private String room;

    @Version
    private long version;

    protected Appointment() {
        // dla JPA
    }

    private Appointment(UUID id, UUID customerId, UUID patientId, UUID veterinarianId,
                        UUID slotId, Instant startTime, int durationMinutes,
                        AppointmentType type, String room) {
        this.id = id;
        this.customerId = customerId;
        this.patientId = patientId;
        this.veterinarianId = veterinarianId;
        this.slotId = slotId;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.type = type;
        this.room = room;
        this.status = AppointmentStatus.CONFIRMED;
    }

    public static Appointment confirm(UUID customerId, UUID patientId, UUID veterinarianId,
                                      AppointmentSlot slot, AppointmentType type, String room) {
        return new Appointment(UUID.randomUUID(), customerId, patientId, veterinarianId,
                slot.getId(), slot.getStartTime(), slot.getDurationMinutes(), type, room);
    }

    public void cancel() {
        if (status == AppointmentStatus.CANCELLED) {
            throw new IllegalAppointmentStateException(id, "already cancelled");
        }
        if (status == AppointmentStatus.COMPLETED) {
            throw new IllegalAppointmentStateException(id, "a completed appointment cannot be cancelled");
        }
        this.status = AppointmentStatus.CANCELLED;
    }

    public void rescheduleTo(AppointmentSlot newSlot) {
        if (status != AppointmentStatus.CONFIRMED && status != AppointmentStatus.RESCHEDULED) {
            throw new IllegalAppointmentStateException(id, "only an active appointment can be rescheduled");
        }
        this.slotId = newSlot.getId();
        this.startTime = newSlot.getStartTime();
        this.durationMinutes = newSlot.getDurationMinutes();
        this.veterinarianId = newSlot.getVeterinarianId();
        this.status = AppointmentStatus.RESCHEDULED;
    }

    public boolean isActive() {
        return status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.RESCHEDULED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public UUID getVeterinarianId() {
        return veterinarianId;
    }

    public UUID getSlotId() {
        return slotId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public AppointmentType getType() {
        return type;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public String getRoom() {
        return room;
    }
}
