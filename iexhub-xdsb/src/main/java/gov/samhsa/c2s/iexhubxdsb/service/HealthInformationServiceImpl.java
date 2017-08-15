package gov.samhsa.c2s.iexhubxdsb.service;

import gov.samhsa.acs.xdsb.common.XdsbDocumentType;
import gov.samhsa.acs.xdsb.registry.wsclient.XdsbRegistryWebServiceClient;
import gov.samhsa.acs.xdsb.registry.wsclient.adapter.XdsbRegistryAdapter;
import gov.samhsa.acs.xdsb.repository.wsclient.XdsbRepositoryWebServiceClient;
import gov.samhsa.c2s.iexhubxdsb.config.IExHubXdsbProperties;
import gov.samhsa.c2s.iexhubxdsb.service.dto.FileExtension;
import gov.samhsa.c2s.iexhubxdsb.service.dto.PatientHealthDataDto;
import gov.samhsa.c2s.iexhubxdsb.service.exception.FileNotFound;
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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;

@Service
@Slf4j
public class HealthInformationServiceImpl implements HealthInformationService {

    private final IExHubXdsbProperties iexhubXdsbProperties;

    private static final String CDAToJsonXSL = "CDA_to_JSON.xsl";

    private static final String ROOT_ATTRIBUTE = "root";


    @Autowired
    public HealthInformationServiceImpl(IExHubXdsbProperties iexhubXdsbProperties) {
        this.iexhubXdsbProperties = iexhubXdsbProperties;
    }


    @Override
    public PatientHealthDataDto getPatientHealthDataFromHIE(String patientId) {
        final String registryEndpoint = iexhubXdsbProperties.getHieos().getXdsbRegistryEndpointURI();
        final String repositoryEndpoint = iexhubXdsbProperties.getHieos().getXdsbRepositoryEndpointURI();
        PatientHealthDataDto patientHealthData = new PatientHealthDataDto();
        String jsonOutput;

        //Step 1: Use PatientId to perform a PIX Query to get the enterprise ID
        //TODO: Remove hardcoded PATIENT_ID when PIX query is ready
        final String PATIENT_ID = "ac4afda28f60407^^^&1.3.6.1.4.1.21367.2005.3.7&ISO";

        //Step 2: Using the enterprise ID, perform XDS.b Registry Operation
        XdsbRegistryAdapter xdsbRegistryAdapter = new XdsbRegistryAdapter(new XdsbRegistryWebServiceClient(registryEndpoint));
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
            XdsbRepositoryWebServiceClient repositoryClient = new XdsbRepositoryWebServiceClient(repositoryEndpoint);
            RetrieveDocumentSetRequestType documentSetRequest = constructDocumentSetRequest(iexhubXdsbProperties.getHieos().getXdsbRepositoryUniqueId(), documents);

            log.info("Calling XdsB Repository");
            RetrieveDocumentSetResponseType retrieveDocumentSetResponse = repositoryClient.documentRepositoryRetrieveDocumentSet(documentSetRequest);
            log.info("Call to XdsB Repository was successful");

            //Step 5: Convert the obtained documents into JSON format
            if (retrieveDocumentSetResponse.getDocumentResponse() != null && retrieveDocumentSetResponse.getDocumentResponse().size() > 0) {
                jsonOutput = convertDocumentResponseToJSON(retrieveDocumentSetResponse.getDocumentResponse());
            } else {
                log.info("Retrieve Document Set transaction found no documents for the given Patient ID");
                throw new NoDocumentsFoundException("Retrieve Document Set transaction found no documents for the given Patient ID");
            }
        }

        //Todo: Convert json to DTO
        return patientHealthData;
    }

    private String convertDocumentResponseToJSON(List<RetrieveDocumentSetResponseType.DocumentResponse> documentResponseList) {
        StringBuilder jsonOutput = new StringBuilder();
        jsonOutput.append("{\"Documents\":[");
        boolean first = true;

        for (RetrieveDocumentSetResponseType.DocumentResponse docResponse : documentResponseList) {
            if (!first) {
                jsonOutput.append(",");
            }
            first = false;
            String documentId = docResponse.getDocumentUniqueId();
            log.info("Processing document ID: " + documentId);

            String mimeType = docResponse.getMimeType();
            if (mimeType.equalsIgnoreCase(MediaType.TEXT_XML_VALUE)) {
                final String filename = iexhubXdsbProperties.getHieos().getDocumentsOutputPath() + "/" + documentId + FileExtension.XML_EXTENSION;
                byte[] documentContents = docResponse.getDocument();
                persistDocument(filename, documentContents);
                Document doc = parseDocument(filename);
                NodeList nodes = convertDocumentToNodeList(doc);

                boolean templateFound = false;
                if (nodes != null && nodes.getLength() > 0) {
                    log.info("Searching for /ClinicalDocument/templateId, document ID: " + documentId);

                    for (int i = 0; i < nodes.getLength(); ++i) {
                        String val = ((Element) nodes.item(i)).getAttribute(ROOT_ATTRIBUTE);
                        if ((val != null) &&
                                (val.compareToIgnoreCase("2.16.840.1.113883.10.20.22.1.2") == 0)) {
                            log.debug("/ClinicalDocument/templateId node found, document ID: " + documentId);

                            log.info("Invoking XSL transform, document ID: " + documentId);
                            DOMSource source = getDOMSource(filename);

                            TransformerFactory transformerFactory = TransformerFactory.newInstance();
                            Transformer transformer;
                            try {
                                transformer = transformerFactory.newTransformer(new StreamSource(CDAToJsonXSL));
                            }
                            catch (TransformerConfigurationException e) {
                                log.error("Unable to create a  Transformer instance" + e.getMessage());
                                throw new FileParseException("Unable to create a  Transformer instance", e);
                            }

                            final String jsonFilename = iexhubXdsbProperties.getHieos().getDocumentsOutputPath() + "/" + documentId + FileExtension.JSON_EXTENSION;
                            File jsonFile = new File(jsonFilename);

                            try (FileOutputStream jsonFileOutStream = new FileOutputStream(jsonFile)) {
                                StreamResult result = new StreamResult(jsonFileOutStream);
                                transformer.transform(source, result);

                                log.info("Successfully transformed CCDA to JSON, filename : " + jsonFilename);

                                jsonOutput.append(new String(readAllBytes(get(jsonFilename))));

                                templateFound = true;
                            }
                            catch (TransformerException e) {
                                log.error("Unable to create a  Transformer instance" + e.getMessage());
                                throw new FileParseException("Unable to transform DOM source to JSON", e);
                            }
                            catch (FileNotFoundException e) {
                                log.error("JSON file:" + jsonFilename + "not found");
                                throw new FileNotFound("JSON file: " + jsonFilename + " not found.", e);
                            }
                            catch (IOException e) {
                                log.error("IO Exception when reading JSON file: " + jsonFilename + e.getMessage());
                                throw new FileParseException("IO Exception when reading JSON file: " + jsonFilename, e);
                            }

                        }

                    }

                } else {
                    log.info("NodeList is NULL.");
                }

                if (!templateFound) {
                    log.error("Document: " + documentId + " retrieved doesn't match required template ID");
                    //TODO: Add to response error messages
                }
            } else {
                log.error("Document: " + documentId + " is not XML");
                //TODO: Add to response error messages
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


    private void persistDocument(String filename, byte[] documentContents) {
        log.info("Persisting document (" + filename + ") to filesystem");


        File file = new File(filename);
        try (FileOutputStream fileOutStream = new FileOutputStream(file)) {
            fileOutStream.write(documentContents);
        }
        catch (FileNotFoundException e) {
            log.error("File(" + filename + ") not found. " + e.getMessage());
            throw new FileNotFound("File(" + filename + ") not found. ", e);
        }
        catch (IOException e) {
            log.error("IOException when writing the file. " + e.getMessage());
            throw new FileParseException("IOException when writing the file: " + filename, e);
        }
    }

    private Document parseDocument(String filename) {

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        Document doc;
        try (FileInputStream fis = new FileInputStream(filename)) {
            dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(fis);
        }
        catch (ParserConfigurationException | SAXException e) {
            log.error(e.getMessage());
            throw new FileParseException(e);
        }
        catch (FileNotFoundException e) {
            log.error("File(" + filename + ") not found. " + e.getMessage());
            throw new FileNotFound("File(" + filename + ") not found. ", e);

        }
        catch (IOException e) {
            log.error("IOException when parsing the file. " + e.getMessage());
            throw new FileParseException("IOException when parsing the file: " + filename, e);
        }

        return doc;
    }

    private NodeList convertDocumentToNodeList(Document doc) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        //set namespace to xpath
        xPath.setNamespaceContext(new NamespaceContext() {
            private final String uri = "urn:hl7-org:v3";
            private final String prefix = "hl7";

            @Override
            public String getNamespaceURI(String prefix) {
                return this.prefix.equals(prefix) ? uri : null;
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return this.uri.equals(namespaceURI) ? this.prefix : null;
            }

            @Override
            public Iterator getPrefixes(String namespaceURI) {
                return null;
            }
        });

        try {
            nodes = (NodeList) xPath.evaluate("/hl7:ClinicalDocument/hl7:templateId",
                    doc.getDocumentElement(),
                    XPathConstants.NODESET);
        }
        catch (XPathExpressionException e) {
            log.error(e.getMessage());
            throw new FileParseException("Error evaluating XPath expression", e);
        }

        return nodes;
    }

    private DOMSource getDOMSource(String filename) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder;
        DOMSource source;
        Document mappedDoc;
        try {
            builder = factory.newDocumentBuilder();
            mappedDoc = builder.parse(new File(filename));
        }
        catch (ParserConfigurationException | SAXException e) {
            log.error(e.getMessage());
            throw new FileParseException("Error parsing the file: " + filename, e);
        }
        catch (IOException e) {
            log.error("IOException when parsing the file: " + e.getMessage());
            throw new FileParseException("IOException when parsing the file: " + filename, e);
        }
        source = new DOMSource(mappedDoc);
        return source;
    }


}
