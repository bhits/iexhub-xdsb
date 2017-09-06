package gov.samhsa.c2s.iexhubxdsb.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class IExHubPixPdqClientException extends RuntimeException  {
    public IExHubPixPdqClientException() {
    }

    public IExHubPixPdqClientException(String message) {
        super(message);
    }
}
