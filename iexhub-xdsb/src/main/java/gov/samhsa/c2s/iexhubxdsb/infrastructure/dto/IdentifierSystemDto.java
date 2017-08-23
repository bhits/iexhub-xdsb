package gov.samhsa.c2s.iexhubxdsb.infrastructure.dto;

import lombok.Data;

@Data
public class IdentifierSystemDto {
    private Long id;
    private String system;
    private String display;
    private String oid;
}
