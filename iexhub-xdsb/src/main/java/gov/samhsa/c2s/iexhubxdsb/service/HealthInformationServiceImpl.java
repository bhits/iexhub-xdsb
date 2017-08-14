package gov.samhsa.c2s.iexhubxdsb.service;

import gov.samhsa.acs.xdsb.registry.common.XdsbDocumentType;
import gov.samhsa.acs.xdsb.registry.wsclient.XdsbRegistryWebServiceClient;
import gov.samhsa.acs.xdsb.registry.wsclient.adapter.XdsbRegistryAdapter;
import gov.samhsa.acs.xdsb.repository.wsclient.XdsbRepositoryWebServiceClient;
import gov.samhsa.c2s.iexhubxdsb.config.IExHubXdsbProperties;
import gov.samhsa.c2s.iexhubxdsb.service.dto.PatientHealthDataDto;
import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import lombok.extern.slf4j.Slf4j;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.IdentifiableType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.LocalizedStringType;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class HealthInformationServiceImpl implements HealthInformationService {

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

        //Step 3: From AdhocQuery Response, extract the document IDs
        List<JAXBElement<? extends IdentifiableType>> documentObjects = adhocQueryResponse.getRegistryObjectList().getIdentifiable();

        if ((documentObjects == null) ||
                (documentObjects.size() == 0)) {
            log.info("No documents found for the given Patient ID");
            return new PatientHealthDataDto();
        } else {
            log.info("Some documents were found in the Registry for the given Patient ID");
            HashMap<String, String> documents = getDocumentsFromDocumentObjects(documentObjects);

            if (documents.size() == 0) {
                log.info("No XDSDocumentEntry documents found for the given Patient ID");
                return new PatientHealthDataDto();
            }
            //Step 4: Using the Document IDs, perform XDS.d Repository call
            XdsbRepositoryWebServiceClient repositoryClient = new XdsbRepositoryWebServiceClient(repositoryEndpoint);
            RetrieveDocumentSetRequestType documentSetRequest = constructDocumentSetRequest(iexhubXdsbProperties.getHieos().getXdsBRepositoryUniqueId(), documents);

            log.info("Calling XdsB Repository");
            RetrieveDocumentSetResponseType retrieveDocumentSetResponse = repositoryClient.documentRepositoryRetrieveDocumentSet(documentSetRequest);
            log.info("Call to XdsB Repository was successful");

            //Step 5: Convert the obtained documents into JSON format

        }
        return null;
    }

    private HashMap<String, String> getDocumentsFromDocumentObjects(List<JAXBElement<? extends IdentifiableType>> documentObjects) {
        HashMap<String, String> documents = new HashMap<>();

        for (JAXBElement identifiable : documentObjects) {
            if (identifiable.getValue() instanceof ExtrinsicObjectType) {
                //Check if "home" attribute is present
                String home = (((ExtrinsicObjectType) identifiable.getValue()).getHome() != null) ? ((ExtrinsicObjectType) identifiable.getValue()).getHome() : null;

                List<ExternalIdentifierType> externalIdentifiers = ((ExtrinsicObjectType) identifiable.getValue()).getExternalIdentifier();

                // Find the ExternalIdentifier that has the "XDSDocumentEntry.uniqueId" value...
                String uniqueId = null;
                for (ExternalIdentifierType externalIdentifier : externalIdentifiers) {

                    boolean foundSearchValue = false;
                    List<LocalizedStringType> localizedStringTypeList = externalIdentifier.getName().getLocalizedString();

                    for (LocalizedStringType temp : localizedStringTypeList) {
                        String searchValue = temp.getValue();
                        if ((searchValue != null) &&
                                (searchValue.equalsIgnoreCase("XDSDocumentEntry.uniqueId"))) {
                            foundSearchValue = true;
                            log.debug("Located XDSDocumentEntry.uniqueId ExternalIdentifier");
                            uniqueId = externalIdentifier.getValue();
                            break;
                        }
                    }
                    if (foundSearchValue) {
                        break;
                    }
                }

                if (uniqueId != null) {
                    documents.put(uniqueId, home);
                    log.debug("Document ID added: " + uniqueId + ", homeCommunityId: " + home);
                }
            } else {
                //TODO: Test for this case
                log.info("Not an ExtrinsicObjectType");
                String home = (((IdentifiableType) identifiable.getValue()).getHome() != null) ? ((IdentifiableType) identifiable.getValue()).getHome() : null;

                documents.put(((IdentifiableType) identifiable.getValue()).getId(), home);
                log.debug("Document ID added: " + ((IdentifiableType) identifiable.getValue()).getId() + ", homeCommunityId: " + home);
            }
        }
        log.info("Number of XDSDocumentEntry documents found = " + documents.size());
        return documents;
    }

    private RetrieveDocumentSetRequestType constructDocumentSetRequest(String repositoryUniqueId,
                                                                       HashMap<String, String> documents) {
        List<RetrieveDocumentSetRequestType.DocumentRequest> documentRequest = new ArrayList<>();
        RetrieveDocumentSetRequestType documentSetRequest = new RetrieveDocumentSetRequestType();

        for (String documentId : documents.keySet()) {
            RetrieveDocumentSetRequestType.DocumentRequest tempDocumentRequest = new RetrieveDocumentSetRequestType.DocumentRequest();
            tempDocumentRequest.setDocumentUniqueId(documentId);
            tempDocumentRequest.setRepositoryUniqueId(repositoryUniqueId);

            if (documents.get(documentId) != null) {
                //HomeCommunityId is present
                tempDocumentRequest.setHomeCommunityId(documents.get(documentId));
            }
            documentRequest.add(tempDocumentRequest);
        }

        documentSetRequest.getDocumentRequest().addAll(documentRequest);
        return documentSetRequest;
    }


}
