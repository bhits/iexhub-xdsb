package gov.samhsa.c2s.iexhubxdsb.service;

import gov.samhsa.acs.common.cxf.ContentTypeRebuildingOutboundSoapInterceptor;
import gov.samhsa.acs.xdsb.common.XdsbDocumentType;
import gov.samhsa.acs.xdsb.registry.wsclient.XdsbRegistryWebServiceClient;
import gov.samhsa.acs.xdsb.registry.wsclient.adapter.XdsbRegistryAdapter;
import gov.samhsa.acs.xdsb.repository.wsclient.XdsbRepositoryWebServiceClient;
import gov.samhsa.acs.xdsb.repository.wsclient.adapter.XdsbRepositoryAdapter;
import gov.samhsa.c2s.common.document.accessor.DocumentAccessor;
import gov.samhsa.c2s.common.document.accessor.DocumentAccessorException;
import gov.samhsa.c2s.common.document.converter.DocumentXmlConverter;
import gov.samhsa.c2s.common.document.transformer.XmlTransformer;
import gov.samhsa.c2s.common.document.transformer.XmlTransformerImpl;
import gov.samhsa.c2s.common.marshaller.SimpleMarshallerException;
import gov.samhsa.c2s.common.marshaller.SimpleMarshallerImpl;
import gov.samhsa.c2s.iexhubxdsb.config.IExHubXdsbProperties;
import gov.samhsa.c2s.iexhubxdsb.service.dto.PatientHealthDataDto;
import gov.samhsa.c2s.iexhubxdsb.service.exception.DocumentNotPublishedException;
import gov.samhsa.c2s.iexhubxdsb.service.exception.FileParseException;
import gov.samhsa.c2s.iexhubxdsb.service.exception.NoDocumentsFoundException;
import gov.samhsa.c2s.iexhubxdsb.service.exception.XdsbRegistryException;
import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import lombok.extern.slf4j.Slf4j;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.IdentifiableType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.LocalizedStringType;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HealthInformationServiceImpl implements HealthInformationService {

    private final IExHubXdsbProperties iexhubXdsbProperties;

    private DocumentAccessor documentAccessor;

    private DocumentXmlConverter documentXmlConverter;

    private static final String CDAToJsonXSL = "CDA_to_JSON.xsl";

    private static final String NODE_ATTRIBUTE_NAME = "root";

    private static final String CCD_TEMPLATE_ID_ROOT_VALUE = "2.16.840.1.113883.10.20.22.1.2";

    private static final String XPATH_EVALUATION_EXPRESSION = "/hl7:ClinicalDocument/hl7:templateId";


    @Autowired
    public HealthInformationServiceImpl(IExHubXdsbProperties iexhubXdsbProperties, DocumentAccessor documentAccessor, DocumentXmlConverter documentXmlConverter) {
        this.iexhubXdsbProperties = iexhubXdsbProperties;
        this.documentAccessor = documentAccessor;
        this.documentXmlConverter = documentXmlConverter;
    }


    @Override
    public String getPatientHealthDataFromHIE(String patientId) {
        final String registryEndpoint = iexhubXdsbProperties.getHieos().getXdsbRegistryEndpointURI();
        final String repositoryEndpoint = iexhubXdsbProperties.getHieos().getXdsbRepositoryEndpointURI();
        PatientHealthDataDto patientHealthData = new PatientHealthDataDto();
        String jsonOutput;

        //Step 1: Use PatientId to perform a PIX Query to get the enterprise ID
        //TODO: Remove hardcoded PATIENT_ID when PIX query is ready
        //Throw PatientDataCannotBeRetrievedException in case of errors
        final String PATIENT_ID = "d3bb3930-7241-11e3-b4f7-00155d3a2124^^^&2.16.840.1.113883.4.357&ISO";

        //Step 2: Using the enterprise ID, perform XDS.b Registry Operation
        final XdsbRegistryAdapter xdsbRegistryAdapter = new XdsbRegistryAdapter(new XdsbRegistryWebServiceClient(registryEndpoint));
        log.info("Calling XdsB Registry");
        AdhocQueryResponse adhocQueryResponse = xdsbRegistryAdapter.registryStoredQuery(PATIENT_ID, XdsbDocumentType.CLINICAL_DOCUMENT);

        //Check for errors
        if ((adhocQueryResponse.getRegistryErrorList() != null) &&
                (adhocQueryResponse.getRegistryErrorList().getRegistryError().size() > 0)) {
            log.info("Call to XdsB registry returned an error");
            log.error("Printing error messages");
            for (RegistryError error : adhocQueryResponse.getRegistryErrorList().getRegistryError()) {
                log.error("Error Code: ", error.getErrorCode());
                log.error("Error Code Context: ", error.getCodeContext());
                log.error("Error Location: ", error.getLocation());
                log.error("Error Severity: ", error.getSeverity());
                log.error("Error Value: ", error.getValue());
            }
            throw new XdsbRegistryException("Call to XdsB registry returned an error. Check iexhub-xdsb.log for details.");
        }
        log.info("XdsB Registry call was successful");

        //Step 3: From AdhocQuery Response, extract the document IDs
        List<JAXBElement<? extends IdentifiableType>> documentObjects = adhocQueryResponse.getRegistryObjectList().getIdentifiable();

        if ((documentObjects == null) ||
                (documentObjects.size() <= 0)) {
            log.info("No documents found for the given Patient ID");
            throw new NoDocumentsFoundException("No documents found for the given Patient ID");
        } else {
            log.info("Some documents were found in the Registry for the given Patient ID");
            HashMap<String, String> documents = getDocumentsFromDocumentObjects(documentObjects);

            if (documents.size() <= 0) {
                log.info("No XDSDocumentEntry documents found for the given Patient ID");
                throw new NoDocumentsFoundException("No XDSDocumentEntry documents found for the given Patient ID");
            }
            //Step 4: Using the Document IDs, perform XDS.d Repository call
            XdsbRepositoryWebServiceClient client = new XdsbRepositoryWebServiceClient(repositoryEndpoint);
            client.setOutInterceptors(Collections.singletonList(new ContentTypeRebuildingOutboundSoapInterceptor()));
            final XdsbRepositoryAdapter xdsbRepositoryAdapter = new XdsbRepositoryAdapter(client, new SimpleMarshallerImpl());
            RetrieveDocumentSetRequestType documentSetRequest = constructDocumentSetRequest(iexhubXdsbProperties.getHieos().getXdsbRepositoryUniqueId(), documents);

            log.info("Calling XdsB Repository");
            RetrieveDocumentSetResponseType retrieveDocumentSetResponse = xdsbRepositoryAdapter.retrieveDocumentSet(documentSetRequest);
            log.info("Call to XdsB Repository was successful");

            //Step 5: Convert the obtained documents into JSON format
            if (retrieveDocumentSetResponse != null && retrieveDocumentSetResponse.getDocumentResponse() != null && retrieveDocumentSetResponse.getDocumentResponse().size() > 0) {
                jsonOutput = convertDocumentResponseToJSON(retrieveDocumentSetResponse.getDocumentResponse());
            } else {
                log.info("Retrieve Document Set transaction found no documents for the given Patient ID");
                throw new NoDocumentsFoundException("Retrieve Document Set transaction found no documents for the given Patient ID");
            }
        }
        return jsonOutput;
    }

    @Override
    public void publishPatientHealthDataToHIE(MultipartFile clinicalDoc) {
        //TODO: Add additional checks as needed when this api is called within C2S
        byte[] documentContent;
        try {
            // extract file content as byte array
            documentContent = clinicalDoc.getBytes();
            log.info("Converted file to byte array.");
        }
        catch (IOException e) {
            log.error("An IOException occurred while invoking file.getBytes from inside the publishPatientHealthDataToHIE method", e);
            throw new DocumentNotPublishedException("An error occurred while attempting to publish the document", e);
        }
        final String repositoryEndpoint = iexhubXdsbProperties.getHieos().getXdsbRepositoryEndpointURI();
        final String openEmpiDomainId = iexhubXdsbProperties.getHieos().getOpenEmpiDomainId();

        XdsbRepositoryWebServiceClient client = new XdsbRepositoryWebServiceClient(repositoryEndpoint);
        client.setOutInterceptors(Collections.singletonList(new ContentTypeRebuildingOutboundSoapInterceptor()));
        final XdsbRepositoryAdapter xdsbRepositoryAdapter = new XdsbRepositoryAdapter(client, new SimpleMarshallerImpl());

        try {
            log.info("Calling XdsB Repository");
            xdsbRepositoryAdapter.documentRepositoryRetrieveDocumentSet(new String(documentContent), openEmpiDomainId, XdsbDocumentType.CLINICAL_DOCUMENT);
            log.info("Call to XdsB Repository was successful. Successfully published the document to HIE.");
        }
        catch (SimpleMarshallerException e) {
            log.error("A SimpleMarshallerException occurred while invoking documentRepositoryRetrieveDocumentSet", e);
            throw new DocumentNotPublishedException("An error occurred while attempting to publish the document", e);
        }

    }

    private String convertDocumentResponseToJSON(List<RetrieveDocumentSetResponseType.DocumentResponse> documentResponseList) {
        StringBuilder jsonOutput = new StringBuilder();
        jsonOutput.append("{\"Documents\":[");
        boolean firstDocument = true;

        for (RetrieveDocumentSetResponseType.DocumentResponse docResponse : documentResponseList) {
            String documentId = docResponse.getDocumentUniqueId();
            log.debug("Processing document ID: " + documentId);

            String mimeType = docResponse.getMimeType();
            if (mimeType.equalsIgnoreCase(MediaType.TEXT_XML_VALUE)) {

                final Document document = documentXmlConverter.loadDocument(new String(docResponse.getDocument()));

                try {
                    List<Node> nodeList = documentAccessor.getNodeListAsStream(document, XPATH_EVALUATION_EXPRESSION).collect(Collectors.toList());

                    if (nodeList != null && nodeList.size() > 0) {

                        final List<Node> selectedNodes = nodeList.stream().filter(node -> node.getAttributes().getNamedItem(NODE_ATTRIBUTE_NAME).getNodeValue().equalsIgnoreCase(CCD_TEMPLATE_ID_ROOT_VALUE))
                                .collect(Collectors.toList());

                        if (selectedNodes.size() > 0) {
                            XmlTransformer xmlTransformer = new XmlTransformerImpl(new SimpleMarshallerImpl());
                            String transformedDocument = xmlTransformer.transform(
                                    document, new ClassPathResource(CDAToJsonXSL).getURI().toString(),
                                    Optional.empty(), Optional.empty());

                            if (!firstDocument) {
                                jsonOutput.append(",");
                            }
                            firstDocument = false;

                            jsonOutput.append(transformedDocument);
                        } else {
                            log.debug("Document(" + documentId + ") retrieved doesn't match required template ID.");
                        }
                    } else {
                        log.debug("NodeList is NULL.");
                    }

                }
                catch (DocumentAccessorException e) {
                    log.error(e.getMessage());
                    throw new FileParseException("Error evaluating XPath expression", e);
                }
                catch (IOException e) {
                    log.error(e.getMessage());
                    throw new FileParseException("Error reading file:" + CDAToJsonXSL, e);
                }
            } else {
                log.error("Document: " + documentId + " is not XML");
            }
        }

        if (jsonOutput.length() > 0) {
            jsonOutput.append("]}");
        }

        return jsonOutput.toString();
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
