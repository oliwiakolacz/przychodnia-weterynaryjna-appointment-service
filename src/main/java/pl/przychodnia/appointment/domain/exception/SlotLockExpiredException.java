package pl.przychodnia.appointment.domain.exception;

import java.util.UUID;

/** Próba potwierdzenia rezerwacji bez ważnej blokady (wygasła lub należy do kogoś innego). */
public class SlotLockExpiredException extends RuntimeException {
    private final UUID slotId;

    public SlotLockExpiredException(UUID slotId) {
        super("Hold for slot %s has expired or is not owned by the caller".formatted(slotId));
        this.slotId = slotId;
    }

    public UUID getSlotId() {
        return slotId;
    }
}
