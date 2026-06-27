package pl.przychodnia.appointment.domain;

import org.junit.jupiter.api.Test;
import pl.przychodnia.appointment.domain.exception.IllegalAppointmentStateException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentTest {

    private static final Instant START = Instant.parse("2026-01-02T09:00:00Z");

    private AppointmentSlot slot(Instant start) {
        return new AppointmentSlot(UUID.randomUUID(), UUID.randomUUID(), start, 30);
    }

    private Appointment confirmedAppointment() {
        return Appointment.confirm(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                slot(START), AppointmentType.CONSULTATION, "1");
    }

    @Test
    void newAppointmentIsConfirmed() {
        assertThat(confirmedAppointment().getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void cancelChangesStatusToCancelled() {
        Appointment a = confirmedAppointment();
        a.cancel();
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void cancelTwiceIsRejected() {
        Appointment a = confirmedAppointment();
        a.cancel();
        assertThatThrownBy(a::cancel).isInstanceOf(IllegalAppointmentStateException.class);
    }

    @Test
    void rescheduleMovesToNewSlot() {
        Appointment a = confirmedAppointment();
        Instant newStart = START.plus(Duration.ofDays(1));
        AppointmentSlot newSlot = slot(newStart);

        a.rescheduleTo(newSlot);

        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.RESCHEDULED);
        assertThat(a.getStartTime()).isEqualTo(newStart);
        assertThat(a.getSlotId()).isEqualTo(newSlot.getId());
    }

    @Test
    void cancelledAppointmentCannotBeRescheduled() {
        Appointment a = confirmedAppointment();
        a.cancel();
        AppointmentSlot newSlot = slot(START.plus(Duration.ofDays(1)));

        assertThatThrownBy(() -> a.rescheduleTo(newSlot))
                .isInstanceOf(IllegalAppointmentStateException.class);
    }
}
