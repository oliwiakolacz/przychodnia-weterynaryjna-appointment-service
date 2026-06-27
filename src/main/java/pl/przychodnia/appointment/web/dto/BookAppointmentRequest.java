package pl.przychodnia.appointment.web.dto;

import jakarta.validation.constraints.NotNull;
import pl.przychodnia.appointment.domain.AppointmentType;
import pl.przychodnia.appointment.service.BookAppointmentCommand;

import java.util.UUID;

public record BookAppointmentRequest(
        @NotNull UUID slotId,
        @NotNull UUID holderId,
        @NotNull UUID customerId,
        @NotNull UUID patientId,
        @NotNull UUID veterinarianId,
        @NotNull AppointmentType type,
        String room) {

    public BookAppointmentCommand toCommand() {
        return new BookAppointmentCommand(slotId, holderId, customerId, patientId,
                veterinarianId, type, room);
    }
}
