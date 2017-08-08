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
public class TreatmentDto {
    private String service;
    private LocalDate serviceStartDate;
    private LocalDate serviceEndDate;
    private List<ProviderDto> providers;
}