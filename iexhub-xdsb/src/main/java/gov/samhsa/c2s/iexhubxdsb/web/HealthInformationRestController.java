package gov.samhsa.c2s.iexhubxdsb.web;

import gov.samhsa.c2s.iexhubxdsb.service.HealthInformationService;
import gov.samhsa.c2s.iexhubxdsb.service.dto.PatientHealthDataDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/patients")
public class HealthInformationRestController {
    private final HealthInformationService healthInfoService;

    public HealthInformationRestController(HealthInformationService healthInfoService) {
        this.healthInfoService = healthInfoService;
    }

    @GetMapping("/{patientId}/health-information")
    public PatientHealthDataDto getPatientHealthDataFromHIE(@PathVariable String patientId) {
        return healthInfoService.getPatientHealthDataFromHIE(patientId);
    }

    @PostMapping("/health-information")
    public void publishPatientHealthDataToHIE(@RequestParam(value = "clinicalDocument") MultipartFile documentFile) {
        healthInfoService.publishPatientHealthDataToHIE(documentFile);
    }

}
