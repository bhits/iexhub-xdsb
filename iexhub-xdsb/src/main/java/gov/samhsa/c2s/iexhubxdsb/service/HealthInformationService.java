package gov.samhsa.c2s.iexhubxdsb.service;

import gov.samhsa.c2s.iexhubxdsb.infrastructure.dto.PatientIdentifierDto;
import org.springframework.web.multipart.MultipartFile;

public interface HealthInformationService {
    /**
     *
     * @param patientId The Patient ID (C2S MRN)
     * @return List of Documents as a JSON string
     */
    String getPatientHealthDataFromHIE(String patientId);

    /**
     *
     * @param clinicalDoc The Clinical Document
     */
    void publishPatientHealthDataToHIE(MultipartFile clinicalDoc, PatientIdentifierDto patientIdentifierDto);

    /**
     *
     * @param patientId
     * @return
     * @throws Exception
     */
    String getFhirResourcesByPaitentid(String patientId) throws Exception;
}
