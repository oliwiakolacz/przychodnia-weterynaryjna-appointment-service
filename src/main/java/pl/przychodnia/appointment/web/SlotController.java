package pl.przychodnia.appointment.web;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.przychodnia.appointment.service.AppointmentService;
import pl.przychodnia.appointment.service.SlotHold;
import pl.przychodnia.appointment.web.dto.AvailableSlotResponse;
import pl.przychodnia.appointment.web.dto.LockSlotRequest;
import pl.przychodnia.appointment.web.dto.SlotHoldResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/slots")
public class SlotController {

    private final AppointmentService appointmentService;

    public SlotController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public List<AvailableSlotResponse> available(
            @RequestParam UUID veterinarianId,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        return appointmentService.availableSlots(veterinarianId, from, to).stream()
                .map(AvailableSlotResponse::from)
                .toList();
    }

    /** FR-APPT-003: tymczasowa blokada slotu. */
    @PostMapping("/{slotId}/holds")
    public SlotHoldResponse hold(@PathVariable UUID slotId, @Valid @RequestBody LockSlotRequest request) {
        SlotHold hold = appointmentService.lockSlot(slotId, request.holderId());
        return SlotHoldResponse.from(hold);
    }
}
