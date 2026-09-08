package com.example.medicalrecordservice.controller;


import com.example.medicalrecordservice.dto.MedicalRecordDto;
import com.example.medicalrecordservice.model.MedicalRecord;
import com.example.medicalrecordservice.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @PostMapping
    public ResponseEntity<MedicalRecord> create(
            @RequestBody MedicalRecordDto dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(medicalRecordService.create(dto));
    }

    @GetMapping
    public ResponseEntity<List<MedicalRecord>> getAll() {

        return ResponseEntity.ok(
                medicalRecordService.getAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalRecord> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                medicalRecordService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalRecord> update(
            @PathVariable Long id,
            @RequestBody MedicalRecordDto dto) {

        return ResponseEntity.ok(
                medicalRecordService.update(id, dto)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        medicalRecordService.delete(id);

        return ResponseEntity.noContent().build();
    }
}