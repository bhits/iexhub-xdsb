package gov.samhsa.c2s.iexhubxdsb.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class UmsClientException extends RuntimeException  {
    public UmsClientException() {
    }

    public UmsClientException(String message) {
        super(message);
    }
}
