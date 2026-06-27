package pl.przychodnia.appointment.event;

import pl.przychodnia.appointment.domain.Appointment;

import java.time.Instant;
import java.util.UUID;

/** Zdarzenia domenowe publikowane na Kafkę (konsumowane m.in. przez Notification Service). */
public final class AppointmentEvents {

    private AppointmentEvents() {
    }

    public sealed interface AppointmentEvent
            permits Created, Cancelled, Rescheduled {
        UUID appointmentId();

        Instant occurredAt();
    }

    public record Created(UUID appointmentId, UUID customerId, UUID patientId,
                          UUID veterinarianId, Instant startTime, Instant occurredAt)
            implements AppointmentEvent {

        public static Created of(Appointment a) {
            return new Created(a.getId(), a.getCustomerId(), a.getPatientId(),
                    a.getVeterinarianId(), a.getStartTime(), Instant.now());
        }
    }

    public record Cancelled(UUID appointmentId, UUID customerId, Instant occurredAt)
            implements AppointmentEvent {

        public static Cancelled of(Appointment a) {
            return new Cancelled(a.getId(), a.getCustomerId(), Instant.now());
        }
    }

    public record Rescheduled(UUID appointmentId, UUID customerId,
                              Instant previousStartTime, Instant newStartTime, Instant occurredAt)
            implements AppointmentEvent {

        public static Rescheduled of(Appointment a, Instant previousStartTime) {
            return new Rescheduled(a.getId(), a.getCustomerId(), previousStartTime,
                    a.getStartTime(), Instant.now());
        }
    }
}
