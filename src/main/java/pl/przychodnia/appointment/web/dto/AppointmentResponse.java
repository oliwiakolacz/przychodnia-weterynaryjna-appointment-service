package pl.przychodnia.appointment.web.dto;

import pl.przychodnia.appointment.domain.Appointment;
import pl.przychodnia.appointment.domain.AppointmentStatus;
import pl.przychodnia.appointment.domain.AppointmentType;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
        UUID id, UUID customerId, UUID patientId, UUID veterinarianId, UUID slotId,
        Instant startTime, int durationMinutes,
        AppointmentType type, AppointmentStatus status, String room) {

    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(a.getId(), a.getCustomerId(), a.getPatientId(),
                a.getVeterinarianId(), a.getSlotId(), a.getStartTime(), a.getDurationMinutes(),
                a.getType(), a.getStatus(), a.getRoom());
    }
}
