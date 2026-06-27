package pl.przychodnia.appointment.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.przychodnia.appointment.service.AppointmentService;
import pl.przychodnia.appointment.web.dto.AppointmentResponse;
import pl.przychodnia.appointment.web.dto.BookAppointmentRequest;
import pl.przychodnia.appointment.web.dto.RescheduleRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /** FR-APPT-001: rezerwacja wizyty. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse book(@Valid @RequestBody BookAppointmentRequest request) {
        return AppointmentResponse.from(appointmentService.book(request.toCommand()));
    }

    /** FR-APPT-002: anulowanie wizyty. */
    @PostMapping("/{id}/cancellation")
    public AppointmentResponse cancel(@PathVariable UUID id) {
        return AppointmentResponse.from(appointmentService.cancel(id));
    }

    /** FR-APPT-002: przesunięcie wizyty. */
    @PostMapping("/{id}/reschedule")
    public AppointmentResponse reschedule(@PathVariable UUID id, @Valid @RequestBody RescheduleRequest request) {
        return AppointmentResponse.from(
                appointmentService.reschedule(id, request.newSlotId(), request.holderId()));
    }
}
