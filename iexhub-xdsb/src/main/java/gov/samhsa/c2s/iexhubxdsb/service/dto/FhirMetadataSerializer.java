package gov.samhsa.c2s.iexhubxdsb.service.dto;

import ca.uhn.fhir.parser.JsonParser;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class FhirMetadataSerializer extends JsonSerializer<FhirMetadataDto> {

    private final JsonParser fhirJsonParser;

    public FhirMetadataSerializer(JsonParser fhirJsonParser) {
        this.fhirJsonParser = fhirJsonParser;
    }

    @Override
    public void serialize(FhirMetadataDto fhirMetadataDto, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        final String json = fhirJsonParser.encodeResourceToString(fhirMetadataDto.getCapabilityStatement());
        jsonGenerator.writeRawValue(json);
    }
}
