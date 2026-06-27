package pl.przychodnia.appointment.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import pl.przychodnia.appointment.domain.AppointmentSlot;
import pl.przychodnia.appointment.event.AppointmentEventPublisher;
import pl.przychodnia.appointment.repository.AppointmentSlotRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dowód obsługi wyścigu o ten sam termin (FR-APPT-003): dwa równoległe żądania
 * blokady tego samego slotu - dokładnie jedno wygrywa (pesymistyczna blokada wiersza).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class SlotLockingConcurrencyIT {

    @MockBean
    private AppointmentEventPublisher publisher;

    @Autowired
    private AppointmentService service;

    @Autowired
    private AppointmentSlotRepository slots;

    @Test
    void onlyOneHolderWinsTheRaceForTheSameSlot() throws Exception {
        UUID slotId = UUID.randomUUID();
        slots.saveAndFlush(new AppointmentSlot(slotId, UUID.randomUUID(),
                Instant.now().plus(Duration.ofDays(1)), 30));

        UUID holderA = UUID.randomUUID();
        UUID holderB = UUID.randomUUID();
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> a = pool.submit(attempt(slotId, holderA, barrier));
            Future<Boolean> b = pool.submit(attempt(slotId, holderB, barrier));

            long winners = List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS))
                    .stream().filter(Boolean::booleanValue).count();

            assertThat(winners).isEqualTo(1L);

            AppointmentSlot slot = slots.findById(slotId).orElseThrow();
            assertThat(slot.getLockedBy()).isIn(holderA, holderB);
            assertThat(slot.isLocked(Instant.now())).isTrue();
        } finally {
            pool.shutdownNow();
        }
    }

    private Callable<Boolean> attempt(UUID slotId, UUID holder, CyclicBarrier barrier) {
        return () -> {
            barrier.await();
            try {
                service.lockSlot(slotId, holder);
                return true;
            } catch (RuntimeException ex) {
                return false;
            }
        };
    }
}
