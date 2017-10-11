package gov.samhsa.c2s.iexhubxdsb.web;

import gov.samhsa.c2s.iexhubxdsb.infrastructure.dto.PatientIdentifierDto;
import gov.samhsa.c2s.iexhubxdsb.service.HealthInformationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class HealthInformationRestController {
    private final HealthInformationService healthInfoService;

    public HealthInformationRestController(HealthInformationService healthInfoService) {
        this.healthInfoService = healthInfoService;
    }

    @GetMapping(value = "/patients/{patientId}/health-information", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getPatientHealthDataFromHIE(@PathVariable String patientId) {
        return healthInfoService.getPatientHealthDataFromHIE(patientId);
    }

    @PostMapping("/patients/health-information")
    public void publishPatientHealthDataToHIE(@RequestParam(value = "clinicalDocument") MultipartFile documentFile,
                                              @RequestParam(value = "patientIdentifier") PatientIdentifierDto patientIdentifierDto) {
        healthInfoService.publishPatientHealthDataToHIE(documentFile, patientIdentifierDto);
    }

    @GetMapping("/Patient/{patientId}/$everything")
    public String getFhirResourcesByPaitentid(@PathVariable String patientId)  {
        return healthInfoService.getFhirResourcesByPaitentid(patientId);
    }
}
