package gov.samhsa.c2s.iexhubxdsb.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class FhirMetadataServiceException extends RuntimeException {
    public FhirMetadataServiceException() {
    }

    public FhirMetadataServiceException(String message) {
        super(message);
    }

    public FhirMetadataServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public FhirMetadataServiceException(Throwable cause) {
        super(cause);
    }

    public FhirMetadataServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
