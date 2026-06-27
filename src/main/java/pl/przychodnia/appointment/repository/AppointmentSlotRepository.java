package pl.przychodnia.appointment.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.przychodnia.appointment.domain.AppointmentSlot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, UUID> {

    /**
     * Pobranie slotu z pesymistyczną blokadą wiersza (SELECT ... FOR UPDATE).
     * Serializuje współbieżne próby zajęcia tego samego terminu (FR-APPT-003) -
     * druga transakcja czeka, po czym widzi już zajęty/zablokowany slot.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AppointmentSlot s where s.id = :id")
    Optional<AppointmentSlot> findByIdForUpdate(@Param("id") UUID id);

    List<AppointmentSlot> findByVeterinarianIdAndAvailableTrueAndStartTimeBetween(
            UUID veterinarianId, Instant from, Instant to);

    @Query("""
            select s from AppointmentSlot s
            where s.available = true
              and s.lockedUntil is not null
              and s.lockedUntil < :now
            """)
    List<AppointmentSlot> findExpiredHolds(@Param("now") Instant now);
}
