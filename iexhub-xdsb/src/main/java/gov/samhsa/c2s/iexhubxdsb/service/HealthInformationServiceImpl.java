package gov.samhsa.c2s.iexhubxdsb.service;

import gov.samhsa.c2s.iexhubxdsb.service.dto.PatientHealthDataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HealthInformationServiceImpl implements HealthInformationService{

    @Override
    public PatientHealthDataDto getPatientHealthDataFromHIE(String patientId) {
        return null;
    }
}
