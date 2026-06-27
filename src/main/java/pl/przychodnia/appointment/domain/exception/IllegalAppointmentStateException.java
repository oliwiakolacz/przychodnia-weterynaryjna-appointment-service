package pl.przychodnia.appointment.domain.exception;

import java.util.UUID;

/** Operacja niedozwolona w obecnym stanie wizyty (np. anulowanie zakończonej wizyty). */
public class IllegalAppointmentStateException extends RuntimeException {
    public IllegalAppointmentStateException(UUID appointmentId, String reason) {
        super("Illegal operation on appointment %s: %s".formatted(appointmentId, reason));
    }
}
