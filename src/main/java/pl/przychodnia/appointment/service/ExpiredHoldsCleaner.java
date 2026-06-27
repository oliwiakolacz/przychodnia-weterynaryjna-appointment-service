package pl.przychodnia.appointment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiredHoldsCleaner {

    private static final Logger log = LoggerFactory.getLogger(ExpiredHoldsCleaner.class);

    private final AppointmentService appointmentService;

    public ExpiredHoldsCleaner(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @Scheduled(fixedDelayString = "${appointment.slot.cleanup-interval}")
    public void releaseExpiredHolds() {
        int released = appointmentService.releaseExpiredHolds();
        if (released > 0) {
            log.info("Released {} expired slot hold(s)", released);
        }
    }
}
