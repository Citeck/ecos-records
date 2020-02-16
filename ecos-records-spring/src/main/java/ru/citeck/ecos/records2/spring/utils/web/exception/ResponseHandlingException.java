package ru.citeck.ecos.records2.spring.utils.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class ResponseHandlingException extends RuntimeException {

    public ResponseHandlingException(Throwable t) {
        super("Cannot write response value. " + t.getLocalizedMessage(), t);
    }
}
