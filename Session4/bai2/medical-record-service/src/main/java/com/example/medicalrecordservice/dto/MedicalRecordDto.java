package com.example.medicalrecordservice.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MedicalRecordDto {

    private Long appointmentId;

    private Long patientId;

    private Long doctorId;

    private String symptoms;

    private String diagnosis;

    private String conclusion;

    private LocalDateTime examinationDate;
}