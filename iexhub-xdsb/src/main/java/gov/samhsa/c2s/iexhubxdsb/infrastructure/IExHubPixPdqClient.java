package gov.samhsa.c2s.iexhubxdsb.infrastructure;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient("iexhub-pix-pdq")
public interface IExHubPixPdqClient {
    @RequestMapping(value = "/patients/{patientId}/mrn-oid/{patientMrnOid}/enterprise-id", method = RequestMethod.GET)
    String getPatientEnterpriseId(@PathVariable("patientId") String patientId,  @PathVariable("patientMrnOid") String patientMrnOid);
}
