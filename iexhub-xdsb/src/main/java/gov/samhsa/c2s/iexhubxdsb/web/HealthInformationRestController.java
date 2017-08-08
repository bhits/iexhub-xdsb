package gov.samhsa.c2s.iexhubxdsb.web;

import gov.samhsa.c2s.iexhubxdsb.service.HealthInformationService;
import gov.samhsa.c2s.iexhubxdsb.service.dto.PatientHealthDataDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/patients/{patientId}")
public class HealthInformationRestController {
    private final HealthInformationService healthInfoService;

    public HealthInformationRestController(HealthInformationService healthInfoService) {
        this.healthInfoService = healthInfoService;
    }

    @GetMapping("/health-information")
    public PatientHealthDataDto getConsentActivities(@PathVariable String patientId) {
        return healthInfoService.getPatientHealthDataFromHIE(patientId);
    }

}
