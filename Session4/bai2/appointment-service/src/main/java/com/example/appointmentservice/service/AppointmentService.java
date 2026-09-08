package com.example.appointmentservice.service;


import com.example.appointmentservice.dto.AppointmentDto;
import com.example.appointmentservice.model.Appointment;
import com.example.appointmentservice.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    public Appointment create(AppointmentDto dto) {

        Appointment appointment = Appointment.builder()
                .patientId(dto.getPatientId())
                .doctorId(dto.getDoctorId())
                .appointmentDate(dto.getAppointmentDate())
                .appointmentTime(dto.getAppointmentTime())
                .reason(dto.getReason())
                .status(dto.getStatus())
                .build();

        return appointmentRepository.save(appointment);
    }

    public List<Appointment> getAll() {
        return appointmentRepository.findAll();
    }

    public Appointment getById(Long id) {

        return appointmentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Appointment not found with id: " + id
                        ));
    }

    public Appointment update(Long id, AppointmentDto dto) {

        Appointment appointment = getById(id);

        appointment.setPatientId(dto.getPatientId());
        appointment.setDoctorId(dto.getDoctorId());
        appointment.setAppointmentDate(dto.getAppointmentDate());
        appointment.setAppointmentTime(dto.getAppointmentTime());
        appointment.setReason(dto.getReason());
        appointment.setStatus(dto.getStatus());

        return appointmentRepository.save(appointment);
    }

    public void delete(Long id) {

        Appointment appointment = getById(id);

        appointmentRepository.delete(appointment);
    }
}