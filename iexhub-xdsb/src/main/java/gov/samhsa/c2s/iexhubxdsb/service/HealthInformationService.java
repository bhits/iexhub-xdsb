package gov.samhsa.c2s.iexhubxdsb.service;

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
    void publishPatientHealthDataToHIE(MultipartFile clinicalDoc);
}
