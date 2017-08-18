package gov.samhsa.c2s.iexhubxdsb.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
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
    private Xdsb xdsb;

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
    }
}