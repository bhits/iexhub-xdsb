package gov.samhsa.c2s.iexhubxdsb.service.dto;

public enum FileExtension {
    XML_EXTENSION(".xml"),
    JSON_EXTENSION(".json");

    private String fileExtension;

    FileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String fileExtension() {
        return fileExtension;
    }
}
