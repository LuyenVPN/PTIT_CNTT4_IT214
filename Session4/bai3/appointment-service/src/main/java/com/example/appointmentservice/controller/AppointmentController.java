package com.example.appointmentservice.controller;

import com.example.appointmentservice.dto.AppointmentDto;
import com.example.appointmentservice.model.Appointment;
import com.example.appointmentservice.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<Appointment> create(
            @RequestBody AppointmentDto dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(appointmentService.create(dto));
    }

    @GetMapping
    public ResponseEntity<List<Appointment>> getAll() {

        return ResponseEntity.ok(
                appointmentService.getAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Appointment> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                appointmentService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Appointment> update(
            @PathVariable Long id,
            @RequestBody AppointmentDto dto) {

        return ResponseEntity.ok(
                appointmentService.update(id, dto)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        appointmentService.delete(id);

        return ResponseEntity.noContent().build();
    }
}