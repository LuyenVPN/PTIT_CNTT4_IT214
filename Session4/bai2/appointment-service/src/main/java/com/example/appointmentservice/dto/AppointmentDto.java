package com.example.appointmentservice.dto;


import com.example.appointmentservice.model.Appointment;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AppointmentDto {

    private Long patientId;

    private Long doctorId;

    private LocalDate appointmentDate;

    private LocalTime appointmentTime;

    private String reason;

    private Appointment.AppointmentStatus status;
}
