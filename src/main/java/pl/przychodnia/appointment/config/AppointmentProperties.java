package pl.przychodnia.appointment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Konfiguracja domenowa serwisu wizyt.
 *
 * @param slot ustawienia slotów (m.in. czas tymczasowej blokady - FR-APPT-003)
 */
@ConfigurationProperties(prefix = "appointment")
public record AppointmentProperties(Slot slot) {

    public record Slot(Duration holdDuration, Duration cleanupInterval) {
        public Slot {
            if (holdDuration == null) {
                holdDuration = Duration.ofMinutes(5);
            }
            if (cleanupInterval == null) {
                cleanupInterval = Duration.ofMinutes(1);
            }
        }
    }
}
