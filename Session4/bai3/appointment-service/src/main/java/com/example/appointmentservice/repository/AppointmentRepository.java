<<<<<<< HEAD
package com.example.appointmentservice.repository;


import com.example.appointmentservice.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {
=======
package com.example.appointmentservice.repository;


import com.example.appointmentservice.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {
>>>>>>> 4101676851925098dd510ffc36d26f9bc968f423
}