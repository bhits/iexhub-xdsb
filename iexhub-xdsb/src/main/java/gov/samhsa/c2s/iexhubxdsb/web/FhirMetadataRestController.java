package gov.samhsa.c2s.iexhubxdsb.web;

import gov.samhsa.c2s.iexhubxdsb.config.IExHubXdsbProperties;
import gov.samhsa.c2s.iexhubxdsb.service.FhirMetadataService;
import gov.samhsa.c2s.iexhubxdsb.service.dto.FhirMetadataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FhirMetadataRestController {
    @Autowired
    private FhirMetadataService fhirMetadataService;

    @GetMapping(value = "/metadata", produces = IExHubXdsbProperties.Fhir.MediaType.APPLICATION_FHIR_JSON_UTF8_VALUE)
    public FhirMetadataDto getMetadata() {
        return fhirMetadataService.generateFhirMetadata();
    }
}
