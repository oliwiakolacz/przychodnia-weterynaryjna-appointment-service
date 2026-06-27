package pl.przychodnia.appointment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.przychodnia.appointment.config.AppointmentProperties;
import pl.przychodnia.appointment.domain.Appointment;
import pl.przychodnia.appointment.domain.AppointmentSlot;
import pl.przychodnia.appointment.domain.AppointmentStatus;
import pl.przychodnia.appointment.domain.AppointmentType;
import pl.przychodnia.appointment.domain.exception.SlotLockExpiredException;
import pl.przychodnia.appointment.event.AppointmentEventPublisher;
import pl.przychodnia.appointment.event.AppointmentEvents;
import pl.przychodnia.appointment.repository.AppointmentRepository;
import pl.przychodnia.appointment.repository.AppointmentSlotRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Mock
    private AppointmentRepository appointments;
    @Mock
    private AppointmentSlotRepository slots;
    @Mock
    private AppointmentEventPublisher publisher;

    private final AppointmentProperties properties =
            new AppointmentProperties(new AppointmentProperties.Slot(Duration.ofMinutes(5), Duration.ofMinutes(1)));
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private AppointmentService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointments, slots, publisher, properties, clock);
    }

    private AppointmentSlot freeSlot(UUID slotId) {
        return new AppointmentSlot(slotId, UUID.randomUUID(), NOW.plus(Duration.ofDays(1)), 30);
    }

    @Test
    void lockSlotSetsHoldUntilNowPlusConfiguredDuration() {
        UUID slotId = UUID.randomUUID();
        UUID holder = UUID.randomUUID();
        AppointmentSlot slot = freeSlot(slotId);
        when(slots.findByIdForUpdate(slotId)).thenReturn(Optional.of(slot));

        SlotHold hold = service.lockSlot(slotId, holder);

        assertThat(hold.holderId()).isEqualTo(holder);
        assertThat(hold.lockedUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
        assertThat(slot.isHeldBy(holder, NOW)).isTrue();
    }

    @Test
    void booksHeldSlotAndPublishesCreatedEvent() {
        UUID slotId = UUID.randomUUID();
        UUID holder = UUID.randomUUID();
        AppointmentSlot slot = freeSlot(slotId);
        slot.lock(holder, NOW.plus(Duration.ofMinutes(5)), NOW);
        when(slots.findByIdForUpdate(slotId)).thenReturn(Optional.of(slot));
        when(appointments.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        BookAppointmentCommand cmd = new BookAppointmentCommand(slotId, holder,
                UUID.randomUUID(), UUID.randomUUID(), slot.getVeterinarianId(),
                AppointmentType.CONSULTATION, "1");

        Appointment result = service.book(cmd);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(slot.isAvailable()).isFalse();
        verify(appointments).save(any(Appointment.class));
        verify(publisher).publish(any(AppointmentEvents.Created.class));
    }

    @Test
    void bookFailsAndPublishesNothingWhenSlotNotHeld() {
        UUID slotId = UUID.randomUUID();
        AppointmentSlot slot = freeSlot(slotId);
        when(slots.findByIdForUpdate(slotId)).thenReturn(Optional.of(slot));

        BookAppointmentCommand cmd = new BookAppointmentCommand(slotId, UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), slot.getVeterinarianId(),
                AppointmentType.CONSULTATION, "1");

        assertThatThrownBy(() -> service.book(cmd)).isInstanceOf(SlotLockExpiredException.class);
        verify(appointments, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void cancelReopensSlotAndPublishesCancelledEvent() {
        UUID slotId = UUID.randomUUID();
        UUID holder = UUID.randomUUID();
        AppointmentSlot slot = freeSlot(slotId);
        slot.lock(holder, NOW.plus(Duration.ofMinutes(5)), NOW);
        slot.confirmBooking(holder, NOW);
        Appointment appointment = Appointment.confirm(UUID.randomUUID(), UUID.randomUUID(),
                slot.getVeterinarianId(), slot, AppointmentType.CONSULTATION, "1");

        when(appointments.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(slots.findByIdForUpdate(slotId)).thenReturn(Optional.of(slot));

        Appointment result = service.cancel(appointment.getId());

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(slot.isAvailable()).isTrue();
        verify(publisher).publish(any(AppointmentEvents.Cancelled.class));
    }
}
