package ru.citeck.ecos.records3.spring.utils.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class RequestHandlingException extends RuntimeException {

    public RequestHandlingException(Throwable t) {
        super("Cannot parse request body. " + t.getLocalizedMessage(), t);
    }
}
