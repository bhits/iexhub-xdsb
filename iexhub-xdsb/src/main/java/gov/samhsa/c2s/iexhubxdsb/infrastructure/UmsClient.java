package gov.samhsa.c2s.iexhubxdsb.infrastructure;

import gov.samhsa.c2s.iexhubxdsb.infrastructure.dto.IdentifierSystemDto;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient("ums")
public interface UmsClient {
    @RequestMapping(value = "/patients/{patientId}/mrn-identifier-system", method = RequestMethod.GET)
    IdentifierSystemDto getPatientMrnIdentifierSystemByPatientId(@PathVariable("patientId") String patientId);
}