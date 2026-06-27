package pl.przychodnia.appointment.event;

import pl.przychodnia.appointment.event.AppointmentEvents.AppointmentEvent;

public interface AppointmentEventPublisher {
    void publish(AppointmentEvent event);
}
