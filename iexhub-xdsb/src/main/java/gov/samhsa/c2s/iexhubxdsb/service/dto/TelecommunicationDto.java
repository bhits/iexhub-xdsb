package gov.samhsa.c2s.iexhubxdsb.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelecommunicationDto {
    private String value;
    private String use;
}