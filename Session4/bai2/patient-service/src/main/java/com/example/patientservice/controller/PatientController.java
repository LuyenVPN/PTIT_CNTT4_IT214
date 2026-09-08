package com.example.patientservice.controller;

import com.example.patientservice.dto.PatientDto;
import com.example.patientservice.model.Patient;
import com.example.patientservice.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    // POST /api/patients
    @PostMapping
    public ResponseEntity<Patient> create(
            @RequestBody PatientDto dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(patientService.create(dto));
    }

    // GET /api/patients
    @GetMapping
    public ResponseEntity<List<Patient>> getAll() {

        return ResponseEntity.ok(patientService.getAll());
    }

    // GET /api/patients/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Patient> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(patientService.getById(id));
    }

    // PUT /api/patients/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Patient> update(
            @PathVariable Long id,
            @RequestBody PatientDto dto) {

        return ResponseEntity.ok(
                patientService.update(id, dto)
        );
    }

    // DELETE /api/patients/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        patientService.delete(id);

        return ResponseEntity.noContent().build();
    }
}