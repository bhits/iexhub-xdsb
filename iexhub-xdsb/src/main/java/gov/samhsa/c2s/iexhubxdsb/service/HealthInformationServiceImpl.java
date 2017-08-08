package gov.samhsa.c2s.iexhubxdsb.service;

import gov.samhsa.c2s.iexhubxdsb.config.IExHubXdsbProperties;
import gov.samhsa.c2s.iexhubxdsb.service.dto.PatientHealthDataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HealthInformationServiceImpl implements HealthInformationService{

    private final IExHubXdsbProperties iexhubXdsbProperties;

    public HealthInformationServiceImpl(IExHubXdsbProperties iexhubXdsbProperties) {
        this.iexhubXdsbProperties = iexhubXdsbProperties;
    }


    @Override
    public PatientHealthDataDto getPatientHealthDataFromHIE(String patientId) {

        boolean tls = false;
        tls = (iexhubXdsbProperties.getHieos().getXdsBRegistryEndpointURI() == null) ? false
                : ((iexhubXdsbProperties.getHieos().getXdsBRegistryEndpointURI().toLowerCase().contains("https") ? true
                : false));
        //Use PatientId to perform a PIX Query to get the enterprise ID

        //Step 2: Using the enterprise ID, perform XDS.d Registry+Repository Operation

        //Step 3: Convert the obtained documents into JSON format


        return null;

    }
}
