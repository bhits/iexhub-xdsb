package gov.samhsa.c2s.iexhubxdsb.service;

import gov.samhsa.c2s.iexhubxdsb.service.dto.PatientHealthDataDto;
import org.springframework.web.multipart.MultipartFile;

public interface HealthInformationService {
    /**
     *
     * @param patientId The patientId
     * @return
     */
    PatientHealthDataDto getPatientHealthDataFromHIE(String patientId);

    /**
     *
     * @param clinicalDoc The Clinical Document
     */
    void publishPatientHealthDataToHIE(MultipartFile clinicalDoc);
}
