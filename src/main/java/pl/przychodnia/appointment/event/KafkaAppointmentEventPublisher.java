package pl.przychodnia.appointment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pl.przychodnia.appointment.event.AppointmentEvents.AppointmentEvent;

@Component
public class KafkaAppointmentEventPublisher implements AppointmentEventPublisher {

    public static final String TOPIC = "appointment.events";

    private static final Logger log = LoggerFactory.getLogger(KafkaAppointmentEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaAppointmentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(AppointmentEvent event) {
        // klucz = id wizyty -> gwarancja kolejności zdarzeń per wizyta
        kafkaTemplate.send(TOPIC, event.appointmentId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} for appointment {}",
                                event.getClass().getSimpleName(), event.appointmentId(), ex);
                    } else {
                        log.debug("Published {} for appointment {}",
                                event.getClass().getSimpleName(), event.appointmentId());
                    }
                });
    }
}
