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
public class TargetPatientDto {
    private LocalDate dob;
    private String gender;
    private String race;
    private String ethnicity;
    private String name;
    private ContactInfoDto contactInfo;
    private List<String> ids;
}
