package pl.przychodnia.appointment.domain.exception;

import java.util.UUID;

/** Slot jest już zarezerwowany lub aktualnie tymczasowo zablokowany przez kogoś innego. */
public class SlotUnavailableException extends RuntimeException {
    private final UUID slotId;

    public SlotUnavailableException(UUID slotId, String reason) {
        super("Slot %s is unavailable: %s".formatted(slotId, reason));
        this.slotId = slotId;
    }

    public UUID getSlotId() {
        return slotId;
    }
}
