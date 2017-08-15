package gov.samhsa.c2s.iexhubxdsb.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class XdsbRegistryException extends RuntimeException {
    public XdsbRegistryException() {}

    public XdsbRegistryException(String message) {
        super(message);
    }

    public XdsbRegistryException(String message, Throwable cause) {
        super(message, cause);
    }

    public XdsbRegistryException(Throwable cause) {
        super(cause);
    }

    public XdsbRegistryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
