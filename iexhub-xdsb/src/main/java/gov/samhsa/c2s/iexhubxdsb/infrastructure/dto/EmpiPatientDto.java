package gov.samhsa.c2s.iexhubxdsb.infrastructure.dto;

import lombok.Data;

@Data
public class EmpiPatientDto {
    private String patientId;
    private String identifier;
    private String identifierType;
}
