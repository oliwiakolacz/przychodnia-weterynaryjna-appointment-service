package pl.przychodnia.appointment.web.dto;

import pl.przychodnia.appointment.service.SlotHold;

import java.time.Instant;
import java.util.UUID;

public record SlotHoldResponse(UUID slotId, UUID holderId, Instant lockedUntil) {

    public static SlotHoldResponse from(SlotHold hold) {
        return new SlotHoldResponse(hold.slotId(), hold.holderId(), hold.lockedUntil());
    }
}
