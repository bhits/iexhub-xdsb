package gov.samhsa.c2s.iexhubxdsb.service;

import gov.samhsa.acs.xdsb.registry.common.XdsbDocumentType;
import gov.samhsa.acs.xdsb.registry.wsclient.XdsbRegistryWebServiceClient;
import gov.samhsa.acs.xdsb.registry.wsclient.adapter.XdsbRegistryAdapter;
import gov.samhsa.c2s.iexhubxdsb.config.IExHubXdsbProperties;
import gov.samhsa.c2s.iexhubxdsb.service.dto.PatientHealthDataDto;
import lombok.extern.slf4j.Slf4j;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HealthInformationServiceImpl implements HealthInformationService{

    private final IExHubXdsbProperties iexhubXdsbProperties;

    private static final String PATIENT_ID = "ac4afda28f60407^^^&1.3.6.1.4.1.21367.2005.3.7&ISO";

    public HealthInformationServiceImpl(IExHubXdsbProperties iexhubXdsbProperties) {
        this.iexhubXdsbProperties = iexhubXdsbProperties;
    }


    @Override
    public PatientHealthDataDto getPatientHealthDataFromHIE(String patientId) {
        String registryEndpoint = iexhubXdsbProperties.getHieos().getXdsBRegistryEndpointURI();
        String repositoryEndpoint = iexhubXdsbProperties.getHieos().getXdsBRepositoryEndpointURI();

        //Step 1: Use PatientId to perform a PIX Query to get the enterprise ID

        //Step 2: Using the enterprise ID, perform XDS.b Registry Operation
        XdsbRegistryAdapter xdsbRegistryAdapter = new XdsbRegistryAdapter(new XdsbRegistryWebServiceClient(registryEndpoint));
        log.info("Calling XdsB Registry");
        AdhocQueryResponse adhocQueryResponse = xdsbRegistryAdapter.registryStoredQuery(PATIENT_ID, XdsbDocumentType.CLINICAL_DOCUMENT);

        //Check for errors
        if ((adhocQueryResponse.getRegistryErrorList() != null) &&
                (adhocQueryResponse.getRegistryErrorList().getRegistryError().size() > 0)) {
            log.info("Calling to XdsB registry return an error");
            log.debug("Printing error messages");
            for (RegistryError error : adhocQueryResponse.getRegistryErrorList().getRegistryError()) {
                log.debug("Error Code: ", error.getErrorCode());
                log.debug("Error Code Context: ", error.getCodeContext());
                log.debug("Error Location: ", error.getLocation());
                log.debug("Error Severity: ", error.getSeverity());
                log.debug("Error Value: ", error.getValue());
            }
            //TODO: Return an exception
            log.info("Returning NULL");
            return null;
        }
        log.info("XdsB Registry call was successful");

        //Step 3: Convert the obtained documents into JSON format


        return null;

    }
}
