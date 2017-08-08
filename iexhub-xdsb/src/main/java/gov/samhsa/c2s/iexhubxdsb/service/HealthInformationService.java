package gov.samhsa.c2s.iexhubxdsb.service;

import gov.samhsa.c2s.iexhubxdsb.service.dto.PatientHealthDataDto;

public interface HealthInformationService {
    /**
     *
     * @param patientId The patientId
     * @return
     */
    PatientHealthDataDto getPatientHealthDataFromHIE(String patientId);
}
