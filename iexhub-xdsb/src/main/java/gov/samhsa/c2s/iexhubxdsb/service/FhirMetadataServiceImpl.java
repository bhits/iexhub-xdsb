package gov.samhsa.c2s.iexhubxdsb.service;

import ca.uhn.fhir.parser.JsonParser;
import gov.samhsa.c2s.iexhubxdsb.config.IExHubXdsbProperties;
import gov.samhsa.c2s.iexhubxdsb.service.dto.FhirMetadataDto;
import gov.samhsa.c2s.iexhubxdsb.service.exception.FhirMetadataServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@Slf4j
public class FhirMetadataServiceImpl implements FhirMetadataService {
    public static final String FHIR_METADATA_JSON_TEMPLATE_CLASSPATH_LOCATION = "fhir/metadata.json";

    @Autowired
    private JsonParser fhirJsonParser;

    @Autowired
    private IExHubXdsbProperties iexhubPixPdqProperties;

    @Override
    public FhirMetadataDto generateFhirMetadata() {
        final IExHubXdsbProperties.Fhir.CapabilityStatement capabilityStatementProperties = iexhubPixPdqProperties.getFhir().getCapabilityStatement();
        final ClassPathResource classPathResource = new ClassPathResource(FHIR_METADATA_JSON_TEMPLATE_CLASSPATH_LOCATION);
        try (final InputStream is = classPathResource.getInputStream()) {
            final String capabilityStatementJsonString = IOUtils.toString(is, StandardCharsets.UTF_8);
            final CapabilityStatement capabilityStatement = (CapabilityStatement) fhirJsonParser.parseResource(capabilityStatementJsonString);
            capabilityStatement.setDate(new Date());
            capabilityStatement.setPublisher(capabilityStatementProperties.getPublisher());
            capabilityStatement.getSoftware().setName(capabilityStatementProperties.getSoftware().getName());
            capabilityStatement.getSoftware().setVersion(capabilityStatementProperties.getSoftware().getVersion());
            capabilityStatement.getImplementation().setDescription(capabilityStatementProperties.getImplementation().getDescription());
            return new FhirMetadataDto(capabilityStatement);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new FhirMetadataServiceException(e.getMessage(), e);
        }
    }
}
