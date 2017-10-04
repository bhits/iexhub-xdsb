package gov.samhsa.c2s.iexhubxdsb.config;

import gov.samhsa.c2s.common.cxf.ContentTypeRebuildingOutboundSoapInterceptor;
import gov.samhsa.c2s.common.document.transformer.XmlTransformer;
import gov.samhsa.c2s.common.marshaller.SimpleMarshaller;
import gov.samhsa.c2s.common.xdsbclient.registry.wsclient.XdsbRegistryWebServiceClient;
import gov.samhsa.c2s.common.xdsbclient.registry.wsclient.adapter.XdsbRegistryAdapter;
import gov.samhsa.c2s.common.xdsbclient.repository.wsclient.XdsbRepositoryWebServiceClient;
import gov.samhsa.c2s.common.xdsbclient.repository.wsclient.adapter.XdsbRepositoryAdapter;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
@ConditionalOnBean(IExHubXdsbProperties.class)
@ConditionalOnProperty(name = "c2s.iexhub-xdsb.xdsb.enabled", havingValue = "true")
@Data
public class XdsbServiceConfig {

    private final IExHubXdsbProperties xdsbProperties;

    private final SimpleMarshaller simpleMarshaller;

    private final XmlTransformer xmlTransformer;

    @Autowired
    public XdsbServiceConfig(IExHubXdsbProperties xdsbProperties, SimpleMarshaller simpleMarshaller, XmlTransformer xmlTransformer) {
        this.xdsbProperties = xdsbProperties;
        this.simpleMarshaller = simpleMarshaller;
        this.xmlTransformer = xmlTransformer;
    }

    @Bean
    XdsbRegistryWebServiceClient xdsbRegistryWebServiceClient() {
        return new XdsbRegistryWebServiceClient(xdsbProperties.getXdsb().getXdsbRegistryEndpointURI());
    }

    @Bean
    public XdsbRegistryAdapter xdsbRegistryAdapter() {
        return new XdsbRegistryAdapter(xdsbRegistryWebServiceClient());
    }

    @Bean
    XdsbRepositoryWebServiceClient xdsbRepositoryWebServiceClient() {
        XdsbRepositoryWebServiceClient client = new XdsbRepositoryWebServiceClient(xdsbProperties.getXdsb().getXdsbRepositoryEndpointURI());
        client.setOutInterceptors(Collections.singletonList(new ContentTypeRebuildingOutboundSoapInterceptor()));
        return client;
    }

    @Bean
    public XdsbRepositoryAdapter xdsbRepositoryAdapter() {
        return new XdsbRepositoryAdapter(xdsbRepositoryWebServiceClient(), simpleMarshaller, xmlTransformer);
    }
}
