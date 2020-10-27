package ru.citeck.ecos.records3.spring.utils.web.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class ResponseHandlingException(t: Throwable) : RuntimeException("Cannot write response value. " + t.localizedMessage, t)
