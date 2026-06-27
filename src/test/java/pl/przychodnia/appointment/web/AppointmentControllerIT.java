package pl.przychodnia.appointment.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pl.przychodnia.appointment.domain.AppointmentSlot;
import pl.przychodnia.appointment.domain.AppointmentType;
import pl.przychodnia.appointment.event.AppointmentEventPublisher;
import pl.przychodnia.appointment.repository.AppointmentSlotRepository;
import pl.przychodnia.appointment.web.dto.BookAppointmentRequest;
import pl.przychodnia.appointment.web.dto.LockSlotRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppointmentControllerIT {

    @MockBean
    private AppointmentEventPublisher publisher;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppointmentSlotRepository slots;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID seedSlot(UUID veterinarianId) {
        UUID slotId = UUID.randomUUID();
        slots.saveAndFlush(new AppointmentSlot(slotId, veterinarianId,
                Instant.now().plus(Duration.ofDays(1)), 30));
        return slotId;
    }

    @Test
    void holdThenBookCreatesConfirmedAppointment() throws Exception {
        UUID vet = UUID.randomUUID();
        UUID slotId = seedSlot(vet);
        UUID holder = UUID.randomUUID();

        mvc.perform(post("/api/v1/slots/{slotId}/holds", slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LockSlotRequest(holder))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holderId").value(holder.toString()));

        BookAppointmentRequest book = new BookAppointmentRequest(slotId, holder,
                UUID.randomUUID(), UUID.randomUUID(), vet, AppointmentType.CONSULTATION, "1");

        mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.slotId").value(slotId.toString()));
    }

    @Test
    void bookingWithoutHoldIsRejectedWithConflict() throws Exception {
        UUID vet = UUID.randomUUID();
        UUID slotId = seedSlot(vet);

        BookAppointmentRequest book = new BookAppointmentRequest(slotId, UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), vet, AppointmentType.CONSULTATION, "1");

        mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isConflict());
    }
}
