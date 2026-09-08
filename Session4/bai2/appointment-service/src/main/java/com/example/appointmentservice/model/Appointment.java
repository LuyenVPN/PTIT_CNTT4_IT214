package com.example.appointmentservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;

    private Long doctorId;

    private LocalDate appointmentDate;

    private LocalTime appointmentTime;

    private String reason;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    public enum AppointmentStatus {
        PENDING,
        CONFIRMED,
        COMPLETED,
        CANCELLED
    }
}