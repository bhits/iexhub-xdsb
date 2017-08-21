package gov.samhsa.c2s.iexhubxdsb.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NoDocumentsFoundException extends RuntimeException {
    public NoDocumentsFoundException() {
    }

    public NoDocumentsFoundException(String message) {
        super(message);
    }

    public NoDocumentsFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoDocumentsFoundException(Throwable cause) {
        super(cause);
    }

    public NoDocumentsFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
