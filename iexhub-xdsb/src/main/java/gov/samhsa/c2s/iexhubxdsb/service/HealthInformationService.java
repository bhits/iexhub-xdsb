package gov.samhsa.c2s.iexhubxdsb.service;

import gov.samhsa.c2s.iexhubxdsb.service.dto.ClinicalDocumentRequest;
import gov.samhsa.c2s.iexhubxdsb.service.dto.PatientHealthDataDto;

public interface HealthInformationService {
    /**
     *
     * @param patientId The patientId
     * @return
     */
    PatientHealthDataDto getPatientHealthDataFromHIE(String patientId);

    /**
     *
     * @param patientId The patientId
     * @param patientDocumentDto The patient's clinical document
     */
    void publishPatientHealthDataToHIE(String patientId, ClinicalDocumentRequest patientDocumentDto);
}
