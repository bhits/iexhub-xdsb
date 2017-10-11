package gov.samhsa.c2s.iexhubxdsb.service.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.IOException;

public class FhirMetadataDeserializer extends JsonDeserializer<FhirMetadataDto> {

    private final ca.uhn.fhir.parser.JsonParser fhirJsonParser;

    public FhirMetadataDeserializer(ca.uhn.fhir.parser.JsonParser fhirJsonParser) {
        this.fhirJsonParser = fhirJsonParser;
    }

    @Override
    public FhirMetadataDto deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        final String jsonAsString = jsonParser.readValueAsTree().toString();
        final IBaseResource iBaseResource = fhirJsonParser.parseResource(jsonAsString);
        return new FhirMetadataDto((CapabilityStatement) iBaseResource);
    }
}
