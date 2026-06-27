package pl.przychodnia.appointment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import pl.przychodnia.appointment.event.KafkaAppointmentEventPublisher;

@Configuration
@Profile("!test")
public class KafkaTopicConfig {

    @Bean
    public NewTopic appointmentEventsTopic() {
        return TopicBuilder.name(KafkaAppointmentEventPublisher.TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
