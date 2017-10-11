package gov.samhsa.c2s.iexhubxdsb.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.JsonParser;
import feign.FeignException;
import gov.samhsa.c2s.common.document.accessor.DocumentAccessor;
import gov.samhsa.c2s.common.document.accessor.DocumentAccessorException;
import gov.samhsa.c2s.common.document.converter.DocumentXmlConverter;
import gov.samhsa.c2s.common.document.transformer.XmlTransformer;
import gov.samhsa.c2s.common.marshaller.SimpleMarshallerException;
import gov.samhsa.c2s.common.xdsbclient.XdsbDocumentType;
import gov.samhsa.c2s.common.xdsbclient.registry.wsclient.adapter.XdsbRegistryAdapter;
import gov.samhsa.c2s.common.xdsbclient.repository.wsclient.adapter.XdsbRepositoryAdapter;
import gov.samhsa.c2s.iexhubxdsb.config.IExHubXdsbProperties;
import gov.samhsa.c2s.iexhubxdsb.infrastructure.IExHubPixPdqClient;
import gov.samhsa.c2s.iexhubxdsb.infrastructure.UmsClient;
import gov.samhsa.c2s.iexhubxdsb.infrastructure.dto.IdentifierSystemDto;
import gov.samhsa.c2s.iexhubxdsb.infrastructure.dto.PatientIdentifierDto;
import gov.samhsa.c2s.iexhubxdsb.service.exception.DocumentNotPublishedException;
import gov.samhsa.c2s.iexhubxdsb.service.exception.FileParseException;
import gov.samhsa.c2s.iexhubxdsb.service.exception.IExHubPixPdqClientException;
import gov.samhsa.c2s.iexhubxdsb.service.exception.NoDocumentsFoundException;
import gov.samhsa.c2s.iexhubxdsb.service.exception.PatientDataCannotBeRetrievedException;
import gov.samhsa.c2s.iexhubxdsb.service.exception.UmsClientException;
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
import org.apache.commons.lang.StringUtils;
import org.fhir.ucum.UcumEssenceService;
import org.fhir.ucum.UcumException;
import org.hl7.fhir.convertors.CCDAConverter;
import org.hl7.fhir.dstu3.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.dstu3.hapi.ctx.IValidationSupport;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Meta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBElement;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HealthInformationServiceImpl implements HealthInformationService {

    private final IExHubXdsbProperties iexhubXdsbProperties;
    private final UmsClient umsClient;
    private final IExHubPixPdqClient iexhubPixPdqClient;
    private DocumentAccessor documentAccessor;
    private DocumentXmlConverter documentXmlConverter;
    private XmlTransformer xmlTransformer;
    private XdsbRegistryAdapter xdsbRegistryAdapter;
    private XdsbRepositoryAdapter xdsbRepositoryAdapter;


    @Autowired
    private JsonParser fhirJsonParser;

    @Autowired
    private FhirContext fhirContext;

    private static final String CDAToJsonXSL = "CDA_to_JSON.xsl";
    private static final String NODE_ATTRIBUTE_NAME = "root";
    private static final String CCD_TEMPLATE_ID_ROOT_VALUE = "2.16.840.1.113883.10.20.22.1.2";
    private static final String XPATH_EVALUATION_EXPRESSION = "/hl7:ClinicalDocument/hl7:templateId";


    @Autowired
    public HealthInformationServiceImpl(IExHubXdsbProperties iexhubXdsbProperties, UmsClient umsClient, IExHubPixPdqClient iexhubPixPdqClient, DocumentAccessor documentAccessor, DocumentXmlConverter documentXmlConverter, XmlTransformer xmlTransformer, XdsbRegistryAdapter xdsbRegistryAdapter, XdsbRepositoryAdapter xdsbRepositoryAdapter) {
        this.iexhubXdsbProperties = iexhubXdsbProperties;
        this.umsClient = umsClient;
        this.iexhubPixPdqClient = iexhubPixPdqClient;
        this.documentAccessor = documentAccessor;
        this.documentXmlConverter = documentXmlConverter;
        this.xmlTransformer = xmlTransformer;
        this.xdsbRegistryAdapter = xdsbRegistryAdapter;
        this.xdsbRepositoryAdapter = xdsbRepositoryAdapter;
    }


    @Override
    public String getPatientHealthDataFromHIE(String patientId) {
        String jsonOutput;
        String searchByPatientId;
        String identifierType = "ISO";
        //Use PatientId to get local Oid from UMS
        IdentifierSystemDto identifier = getPatientIdentifier(patientId);

        String oId = identifier.getOid();
        if (identifier.getOid().toLowerCase().contains("urn:oid:")) {
            oId = StringUtils.substringAfter(oId, "urn:oid:");
        }

        if(iexhubXdsbProperties.getXdsb().isGetHealthDataBasedOnEnterpriseId()){
            PatientIdentifierDto patientEnterpriseId = getEnterprisePatientId(patientId, oId);
            identifierType = patientEnterpriseId.getIdentifierType();
            searchByPatientId = patientEnterpriseId.getPatientId() + "^^^&" + patientEnterpriseId.getIdentifier() + "&" + patientEnterpriseId.getIdentifierType();
        } else {
            //Convert patientId to the format: d3bb3930-7241-11e3-b4f7-00155d3a2124^^^&2.16.840.1.113883.4.357&ISO
            searchByPatientId = patientId + "^^^&" + oId + "&" + identifierType;
        }

        log.info("Calling XdsB Registry");
        AdhocQueryResponse adhocQueryResponse = xdsbRegistryAdapter.registryStoredQuery(searchByPatientId, XdsbDocumentType.CLINICAL_DOCUMENT);

        //Check for errors
        if ((adhocQueryResponse.getRegistryErrorList() != null) &&
                (adhocQueryResponse.getRegistryErrorList().getRegistryError().size() > 0)) {
            logErrorMessages(adhocQueryResponse.getRegistryErrorList().getRegistryError());
            throw new XdsbRegistryException("Call to XdsB registry returned an error. Check iexhub-xdsb.log for details.");
        }
        log.info("XdsB Registry call was successful");

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
            //Perform XDS.b Repository call
            RetrieveDocumentSetRequestType documentSetRequest = constructDocumentSetRequest(iexhubXdsbProperties.getXdsb().getXdsbRepositoryUniqueId(), documents);

            log.info("Calling XdsB Repository");
            RetrieveDocumentSetResponseType retrieveDocumentSetResponse = xdsbRepositoryAdapter.retrieveDocumentSet(documentSetRequest);
            log.info("Call to XdsB Repository was successful");

            //Convert the obtained documents into JSON format
            if (retrieveDocumentSetResponse != null && retrieveDocumentSetResponse.getDocumentResponse() != null && retrieveDocumentSetResponse.getDocumentResponse().size() > 0) {
                log.info("Converting document found in XdsB Repository to JSON");
                jsonOutput = convertDocumentResponseToJSON(retrieveDocumentSetResponse.getDocumentResponse());
            } else {
                log.info("Retrieve Document Set transaction found no documents for the given Patient ID");
                throw new NoDocumentsFoundException("Retrieve Document Set transaction found no documents for the given Patient ID");
            }
        }
        return jsonOutput;
    }

    @Override
    public void publishPatientHealthDataToHIE(MultipartFile clinicalDoc, PatientIdentifierDto patientIdentifierDto) {
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

        try {
            log.info("Calling XdsB Repository");
            xdsbRepositoryAdapter.documentRepositoryRetrieveDocumentSet(new String(documentContent), iexhubXdsbProperties.getXdsb().getHomeCommunityId(), XdsbDocumentType.CLINICAL_DOCUMENT, patientIdentifierDto.getIdentifierType());
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
                String home = (((ExtrinsicObjectType) identifiable.getValue()).getHome() != null) ? ((ExtrinsicObjectType) identifiable.getValue()).getHome() : null;

                List<ExternalIdentifierType> externalIdentifiers = ((ExtrinsicObjectType) identifiable.getValue()).getExternalIdentifier();

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
                tempDocumentRequest.setHomeCommunityId(documents.get(documentId));
            }
            documentRequest.add(tempDocumentRequest);
        }

        documentSetRequest.getDocumentRequest().addAll(documentRequest);
        return documentSetRequest;
    }

    private void logErrorMessages(List<oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError> errorList) {
        log.info("Call to XdsB registry returned an error");
        log.error("Printing error messages");
        for (RegistryError error : errorList) {
            log.error("Error Code: ", error.getErrorCode());
            log.error("Error Code Context: ", error.getCodeContext());
            log.error("Error Location: ", error.getLocation());
            log.error("Error Severity: ", error.getSeverity());
            log.error("Error Value: ", error.getValue());
        }
    }

    private IdentifierSystemDto getPatientIdentifier(String patientId) {
        log.info("Fetching Patient MRN Identifier System from UMS...");
        try {
            //patientId is MRN, not Patient.id
            IdentifierSystemDto identifier = umsClient.getPatientMrnIdentifierSystemByPatientId(patientId);
            log.info("Found Patient MRN Identifier System from UMS");
            return identifier;
        }
        catch (FeignException fe) {
            if (fe.status() == 404) {
                log.error("UMS client returned a 404 - NOT FOUND status, indicating no patient was found for the specified patientMrn", fe);
                throw new PatientDataCannotBeRetrievedException("No patient was found for the specified patientID(MRN)");
            } else {
                log.error("UMS client returned an unexpected instance of FeignException", fe);
                throw new UmsClientException("An unknown error occurred while attempting to communicate with UMS");
            }
        }
    }

    private PatientIdentifierDto getEnterprisePatientId(String patientId, String oid) {
        log.info("Fetching Patient EnterpriseId from IExHubPixPdq...");
        try {
            //patientId is MRN, not Patient.id
            PatientIdentifierDto enterprisePatientId = iexhubPixPdqClient.getPatientEnterpriseId(patientId, oid);
            log.info("Found Patient EnterpriseId from IExHubPixPdq.");
            return enterprisePatientId;
        }
        catch (FeignException fe) {
            if (fe.status() == 404) {
                log.error("IExHubPixPdq client returned a 404 - NOT FOUND status, indicating no patient was found for the specified patientMrn", fe);
                throw new PatientDataCannotBeRetrievedException("No patient was found for the specified patientID(MRN)");
            } else {
                log.error("IExHubPixPdq client returned an unexpected instance of FeignException", fe);
                throw new IExHubPixPdqClientException("An unknown error occurred while attempting to communicate with IExHubPixPdq");
            }
        }
    }

    @Override
    public String getFhirResourcesByPaitentid(String patientId){

        log.info("Calling XdsB Registry");
        AdhocQueryResponse adhocQueryResponse = xdsbRegistryAdapter.registryStoredQuery(patientId, XdsbDocumentType.CLINICAL_DOCUMENT);

        //Check for errors
        if ((adhocQueryResponse.getRegistryErrorList() != null) &&
                (adhocQueryResponse.getRegistryErrorList().getRegistryError().size() > 0)) {
            logErrorMessages(adhocQueryResponse.getRegistryErrorList().getRegistryError());
            throw new XdsbRegistryException("Call to XdsB registry returned an error. Check iexhub-xdsb.log for details.");
        }
        log.info("XdsB Registry call was successful");

        List<JAXBElement<? extends IdentifiableType>> documentObjects = adhocQueryResponse.getRegistryObjectList().getIdentifiable();
        Bundle bundle = new Bundle();

        if ((documentObjects == null) ||
                (documentObjects.size() <= 0)) {
            bundle.setTotal(0);
        } else {
            log.info("Some documents were found in the Registry for the given Patient ID");
            HashMap<String, String> documents = getDocumentsFromDocumentObjects(documentObjects);

            if (documents.size() <= 0) {
                log.info("No XDSDocumentEntry documents found for the given Patient ID");
                throw new NoDocumentsFoundException("No XDSDocumentEntry documents found for the given Patient ID");
            }
            //Perform XDS.b Repository call
            RetrieveDocumentSetRequestType documentSetRequest = constructDocumentSetRequest(iexhubXdsbProperties.getXdsb().getXdsbRepositoryUniqueId(), documents);

            log.info("Calling XdsB Repository");
            RetrieveDocumentSetResponseType retrieveDocumentSetResponse = xdsbRepositoryAdapter.retrieveDocumentSet(documentSetRequest);
            log.info("Call to XdsB Repository was successful");

            if (retrieveDocumentSetResponse != null && retrieveDocumentSetResponse.getDocumentResponse() != null && retrieveDocumentSetResponse.getDocumentResponse().size() > 0) {
                log.info("Converting document found in XdsB Repository to FHIR JSON");
                bundle = convertDocumentResponseToFhir(retrieveDocumentSetResponse.getDocumentResponse());
                bundle.setTotal(retrieveDocumentSetResponse.getDocumentResponse().size());
            } else {
                log.info("Retrieve Document Set transaction found no documents for the given Patient ID");
                bundle.setTotal(0);
            }
        }
        Meta meta = new Meta();
        meta.setLastUpdated(new Date());
        bundle.setType(Bundle.BundleType.SEARCHSET).setId(UUID.randomUUID().toString()).setMeta(meta);
        return fhirJsonParser.setPrettyPrint(true).encodeResourceToString(bundle);
    }

    private Bundle convertDocumentResponseToFhir(List<RetrieveDocumentSetResponseType.DocumentResponse> documentResponseList) {

        IValidationSupport validationSupport = (IValidationSupport) fhirContext.getValidationSupport();

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("ucum-essence.xml");

        Bundle bundle = new Bundle();
        try {
            CCDAConverter ccdaConverter = new CCDAConverter(new UcumEssenceService(inputStream), new HapiWorkerContext(fhirContext, validationSupport));
            documentResponseList.stream().forEach(documentResponse -> {
                        try {
                            bundle.addEntry().setResource(ccdaConverter.convert(new ByteArrayInputStream(documentResponse.getDocument())));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
            );
        } catch (UcumException e) {
            e.printStackTrace();
        }
        return bundle;
    }
}
