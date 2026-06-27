package pl.przychodnia.appointment.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LockSlotRequest(@NotNull UUID holderId) {
}
