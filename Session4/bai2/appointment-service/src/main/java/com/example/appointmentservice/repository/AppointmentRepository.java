package com.example.appointmentservice.repository;


import com.example.appointmentservice.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {
}