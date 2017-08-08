package gov.samhsa.c2s.iexhubxdsb.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CdaDocumentDto {
    private LocalDate date;
    private String type;
    private String id;
    private String title;
    private TargetPatientDto targetPatient;
    private TreatmentDto treatment;
    private List<AuthorDto> authors;
    private List<SectionDto> sections;
}

