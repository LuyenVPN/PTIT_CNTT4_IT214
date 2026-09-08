package com.example.doctorservice.controller;

import com.example.doctorservice.dto.DoctorDto;
import com.example.doctorservice.model.Doctor;
import com.example.doctorservice.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping
    public ResponseEntity<Doctor> create(
            @RequestBody DoctorDto dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(doctorService.create(dto));
    }

    @GetMapping
    public ResponseEntity<List<Doctor>> getAll() {

        return ResponseEntity.ok(doctorService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Doctor> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                doctorService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Doctor> update(
            @PathVariable Long id,
            @RequestBody DoctorDto dto) {

        return ResponseEntity.ok(
                doctorService.update(id, dto)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        doctorService.delete(id);

        return ResponseEntity.noContent().build();
    }
}