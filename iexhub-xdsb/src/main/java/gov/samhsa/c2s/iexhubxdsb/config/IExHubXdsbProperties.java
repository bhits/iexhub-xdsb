package gov.samhsa.c2s.iexhubxdsb.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotNull;

@Configuration
@ConfigurationProperties(prefix = "c2s.iexhub-xdsb")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IExHubXdsbProperties {
    @NotNull
    private Hieos hieos;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Hieos {

        @NotNull
        private boolean enabled;

        @NotEmpty
        private String xdsbRegistryEndpointURI;

        @NotEmpty
        private String xdsbRepositoryEndpointURI;

        @NotEmpty
        private String xdsbRepositoryUniqueId;

        @NotEmpty
        private String openEmpiDomainId;
    }
}