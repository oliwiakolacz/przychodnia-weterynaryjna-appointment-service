package pl.przychodnia.appointment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import pl.przychodnia.appointment.domain.exception.SlotLockExpiredException;
import pl.przychodnia.appointment.domain.exception.SlotUnavailableException;

import java.time.Instant;
import java.util.UUID;

/**
 * Dostępny termin (slot) w grafiku lekarza.
 * <p>
 * Cykl życia: wolny (available) -> tymczasowo zablokowany (lockedBy/lockedUntil)
 * -> zarezerwowany (available=false). Blokada realizuje FR-APPT-003.
 * {@code @Version} chroni przed zgubionym zapisem przy współbieżności.
 */
@Entity
@Table(name = "appointment_slot")
public class AppointmentSlot {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID veterinarianId;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private int durationMinutes;

    /** false = slot jest już zarezerwowany przez potwierdzoną wizytę. */
    @Column(nullable = false)
    private boolean available = true;

    private UUID lockedBy;

    private Instant lockedUntil;

    @Version
    private long version;

    protected AppointmentSlot() {
        // dla JPA
    }

    public AppointmentSlot(UUID id, UUID veterinarianId, Instant startTime, int durationMinutes) {
        this.id = id;
        this.veterinarianId = veterinarianId;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.available = true;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public boolean isHeldBy(UUID who, Instant now) {
        return isLocked(now) && who.equals(lockedBy);
    }

    /**
     * Zakłada tymczasową blokadę na slot do {@code until} dla użytkownika {@code who} (FR-APPT-003).
     * Ponowna blokada przez tego samego właściciela przedłuża hold (idempotentna).
     */
    public void lock(UUID who, Instant until, Instant now) {
        if (!available) {
            throw new SlotUnavailableException(id, "already booked");
        }
        if (isLocked(now) && !who.equals(lockedBy)) {
            throw new SlotUnavailableException(id, "temporarily held by another user");
        }
        this.lockedBy = who;
        this.lockedUntil = until;
    }

    /**
     * Finalizuje rezerwację: wymaga ważnej blokady należącej do {@code who}.
     * Po sukcesie slot jest zajęty (available=false), a hold czyszczony.
     */
    public void confirmBooking(UUID who, Instant now) {
        if (!available) {
            throw new SlotUnavailableException(id, "already booked");
        }
        if (!isHeldBy(who, now)) {
            throw new SlotLockExpiredException(id);
        }
        this.available = false;
        this.lockedBy = null;
        this.lockedUntil = null;
    }

    /** Zwalnia tymczasową blokadę (np. po wygaśnięciu holdu) bez zmiany dostępności. */
    public void releaseHold() {
        this.lockedBy = null;
        this.lockedUntil = null;
    }

    /** Przywraca slot do puli wolnych (np. po anulowaniu wizyty - FR-APPT-002). */
    public void reopen() {
        this.available = true;
        this.lockedBy = null;
        this.lockedUntil = null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVeterinarianId() {
        return veterinarianId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public boolean isAvailable() {
        return available;
    }

    public UUID getLockedBy() {
        return lockedBy;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public long getVersion() {
        return version;
    }
}
