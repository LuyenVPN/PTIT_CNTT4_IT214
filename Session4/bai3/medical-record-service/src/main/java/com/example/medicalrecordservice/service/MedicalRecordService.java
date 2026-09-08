package com.example.medicalrecordservice.service;


import com.example.medicalrecordservice.dto.MedicalRecordDto;
import com.example.medicalrecordservice.model.MedicalRecord;
import com.example.medicalrecordservice.repository.MedicalRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;

    public MedicalRecord create(MedicalRecordDto dto) {

        MedicalRecord record = MedicalRecord.builder()
                .appointmentId(dto.getAppointmentId())
                .patientId(dto.getPatientId())
                .doctorId(dto.getDoctorId())
                .symptoms(dto.getSymptoms())
                .diagnosis(dto.getDiagnosis())
                .conclusion(dto.getConclusion())
                .examinationDate(dto.getExaminationDate())
                .build();

        return medicalRecordRepository.save(record);
    }

    public List<MedicalRecord> getAll() {
        return medicalRecordRepository.findAll();
    }

    public MedicalRecord getById(Long id) {

        return medicalRecordRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Medical record not found with id: " + id
                        ));
    }

    public MedicalRecord update(
            Long id,
            MedicalRecordDto dto) {

        MedicalRecord record = getById(id);

        record.setAppointmentId(dto.getAppointmentId());
        record.setPatientId(dto.getPatientId());
        record.setDoctorId(dto.getDoctorId());
        record.setSymptoms(dto.getSymptoms());
        record.setDiagnosis(dto.getDiagnosis());
        record.setConclusion(dto.getConclusion());
        record.setExaminationDate(dto.getExaminationDate());

        return medicalRecordRepository.save(record);
    }

    public void delete(Long id) {

        MedicalRecord record = getById(id);

        medicalRecordRepository.delete(record);
    }
}
