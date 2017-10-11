package gov.samhsa.c2s.iexhubxdsb.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Configuration
@ConfigurationProperties(prefix = "c2s.iexhub-xdsb")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IExHubXdsbProperties {
    @NotNull
    private Xdsb xdsb;

    @NotNull
    @Valid
    private Fhir fhir;

    @Data
    public static class Fhir {
        @NotNull
        @Valid
        private CapabilityStatement capabilityStatement;

        public interface MediaType {
            String APPLICATION_FHIR_JSON_UTF8_VALUE = "application/fhir+json;charset=UTF-8";
        }

        @Data
        public static class CapabilityStatement {
            @NotBlank
            private String publisher;
            @NotNull
            @Valid
            private Software software;
            @NotNull
            @Valid
            private Implementation implementation;

            @Data
            public static class Software {
                @NotBlank
                private String name;
                @NotBlank
                private String version;
            }

            @Data
            public static class Implementation {
                @NotBlank
                private String description;
            }
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Xdsb {

        @NotNull
        private boolean enabled;

        @NotBlank
        private String xdsbRegistryEndpointURI;

        @NotBlank
        private String xdsbRepositoryEndpointURI;

        @NotBlank
        private String xdsbRepositoryUniqueId;

        @NotBlank
        private String homeCommunityId;

        @NotNull
        private boolean getHealthDataBasedOnEnterpriseId;
    }
}