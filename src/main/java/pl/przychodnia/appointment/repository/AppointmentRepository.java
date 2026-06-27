package pl.przychodnia.appointment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.przychodnia.appointment.domain.Appointment;

import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
}
