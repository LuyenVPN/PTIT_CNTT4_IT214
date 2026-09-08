package com.example.patientservice.service;

import com.example.patientservice.dto.PatientDto;
import com.example.patientservice.model.Patient;
import com.example.patientservice.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    // CREATE
    public Patient create(PatientDto dto) {

        Patient patient = Patient.builder()
                .fullName(dto.getFullName())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .insuranceId(dto.getInsuranceId())
                .build();

        return patientRepository.save(patient);
    }

    // READ ALL
    public List<Patient> getAll() {
        return patientRepository.findAll();
    }

    // READ BY ID
    public Patient getById(Long id) {

        return patientRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Patient not found with id: " + id));
    }

    // UPDATE
    public Patient update(Long id, PatientDto dto) {

        Patient patient = getById(id);

        patient.setFullName(dto.getFullName());
        patient.setDateOfBirth(dto.getDateOfBirth());
        patient.setGender(dto.getGender());
        patient.setPhone(dto.getPhone());
        patient.setAddress(dto.getAddress());
        patient.setInsuranceId(dto.getInsuranceId());

        return patientRepository.save(patient);
    }

    // DELETE
    public void delete(Long id) {

        Patient patient = getById(id);

        patientRepository.delete(patient);
    }
}