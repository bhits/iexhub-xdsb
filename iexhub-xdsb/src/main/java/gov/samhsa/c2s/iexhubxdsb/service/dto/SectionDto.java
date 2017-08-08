package gov.samhsa.c2s.iexhubxdsb.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionDto {
    private String title;
    private String content;
    private String contentMimeType;
    private AuthorDto author;
    private String m_ClinicalStatements;
    private String beid;
}
