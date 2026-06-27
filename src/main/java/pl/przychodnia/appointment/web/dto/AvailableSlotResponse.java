package pl.przychodnia.appointment.web.dto;

import pl.przychodnia.appointment.domain.AppointmentSlot;

import java.time.Instant;
import java.util.UUID;

public record AvailableSlotResponse(UUID slotId, UUID veterinarianId,
                                    Instant startTime, int durationMinutes) {

    public static AvailableSlotResponse from(AppointmentSlot slot) {
        return new AvailableSlotResponse(slot.getId(), slot.getVeterinarianId(),
                slot.getStartTime(), slot.getDurationMinutes());
    }
}
