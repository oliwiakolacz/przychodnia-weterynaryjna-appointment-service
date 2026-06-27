package pl.przychodnia.appointment.domain;

import org.junit.jupiter.api.Test;
import pl.przychodnia.appointment.domain.exception.SlotLockExpiredException;
import pl.przychodnia.appointment.domain.exception.SlotUnavailableException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentSlotTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private AppointmentSlot freeSlot() {
        return new AppointmentSlot(UUID.randomUUID(), UUID.randomUUID(),
                NOW.plus(Duration.ofDays(1)), 30);
    }

    @Test
    void locksFreeSlot() {
        AppointmentSlot slot = freeSlot();
        UUID holder = UUID.randomUUID();

        slot.lock(holder, NOW.plus(Duration.ofMinutes(5)), NOW);

        assertThat(slot.isLocked(NOW)).isTrue();
        assertThat(slot.isHeldBy(holder, NOW)).isTrue();
        assertThat(slot.isAvailable()).isTrue();
    }

    @Test
    void rejectsLockHeldByAnotherUser() {
        AppointmentSlot slot = freeSlot();
        slot.lock(UUID.randomUUID(), NOW.plus(Duration.ofMinutes(5)), NOW);

        assertThatThrownBy(() -> slot.lock(UUID.randomUUID(), NOW.plus(Duration.ofMinutes(5)), NOW))
                .isInstanceOf(SlotUnavailableException.class);
    }

    @Test
    void sameHolderCanExtendHold() {
        AppointmentSlot slot = freeSlot();
        UUID holder = UUID.randomUUID();
        slot.lock(holder, NOW.plus(Duration.ofMinutes(2)), NOW);

        slot.lock(holder, NOW.plus(Duration.ofMinutes(5)), NOW);

        assertThat(slot.getLockedUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
    }

    @Test
    void confirmsBookingForValidHold() {
        AppointmentSlot slot = freeSlot();
        UUID holder = UUID.randomUUID();
        slot.lock(holder, NOW.plus(Duration.ofMinutes(5)), NOW);

        slot.confirmBooking(holder, NOW);

        assertThat(slot.isAvailable()).isFalse();
        assertThat(slot.getLockedBy()).isNull();
    }

    @Test
    void rejectsConfirmWithoutHold() {
        AppointmentSlot slot = freeSlot();

        assertThatThrownBy(() -> slot.confirmBooking(UUID.randomUUID(), NOW))
                .isInstanceOf(SlotLockExpiredException.class);
    }

    @Test
    void rejectsConfirmAfterHoldExpired() {
        AppointmentSlot slot = freeSlot();
        UUID holder = UUID.randomUUID();
        slot.lock(holder, NOW.plus(Duration.ofMinutes(5)), NOW);

        Instant afterExpiry = NOW.plus(Duration.ofMinutes(6));
        assertThatThrownBy(() -> slot.confirmBooking(holder, afterExpiry))
                .isInstanceOf(SlotLockExpiredException.class);
    }

    @Test
    void rejectsLockOnBookedSlot() {
        AppointmentSlot slot = freeSlot();
        UUID holder = UUID.randomUUID();
        slot.lock(holder, NOW.plus(Duration.ofMinutes(5)), NOW);
        slot.confirmBooking(holder, NOW);

        assertThatThrownBy(() -> slot.lock(UUID.randomUUID(), NOW.plus(Duration.ofMinutes(5)), NOW))
                .isInstanceOf(SlotUnavailableException.class);
    }

    @Test
    void reopenMakesSlotAvailableAgain() {
        AppointmentSlot slot = freeSlot();
        UUID holder = UUID.randomUUID();
        slot.lock(holder, NOW.plus(Duration.ofMinutes(5)), NOW);
        slot.confirmBooking(holder, NOW);

        slot.reopen();

        assertThat(slot.isAvailable()).isTrue();
        assertThat(slot.isLocked(NOW)).isFalse();
    }
}
