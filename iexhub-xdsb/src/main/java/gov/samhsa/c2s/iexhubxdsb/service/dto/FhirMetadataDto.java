package gov.samhsa.c2s.iexhubxdsb.service.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.springframework.util.Assert;

@JsonDeserialize(using = FhirMetadataDeserializer.class)
@JsonSerialize(using = FhirMetadataSerializer.class)
public class FhirMetadataDto {
    private final CapabilityStatement capabilityStatement;

    public FhirMetadataDto(CapabilityStatement capabilityStatement) {
        Assert.notNull(capabilityStatement, "capabilityStatement cannot be null");
        this.capabilityStatement = capabilityStatement;
    }

    public CapabilityStatement getCapabilityStatement() {
        return capabilityStatement;
    }
}
