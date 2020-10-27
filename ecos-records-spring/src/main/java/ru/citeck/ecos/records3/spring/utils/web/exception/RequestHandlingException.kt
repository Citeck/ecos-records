package ru.citeck.ecos.records3.spring.utils.web.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class RequestHandlingException(t: Throwable) : RuntimeException("Cannot parse request body. " + t.localizedMessage, t)
