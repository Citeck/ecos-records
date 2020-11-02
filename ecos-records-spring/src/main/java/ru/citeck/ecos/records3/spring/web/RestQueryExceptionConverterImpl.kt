package ru.citeck.ecos.records3.spring.web

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang.StringUtils
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import ru.citeck.ecos.records2.rest.RestQueryException
import ru.citeck.ecos.records2.rest.RestQueryExceptionConverter

@Component
class RestQueryExceptionConverterImpl : RestQueryExceptionConverter {

    override fun convert(msg: String, e: Exception?): RestQueryException {
        if (e == null) {
            return RestQueryException(msg)
        }
        if (e is HttpServerErrorException) {
            val restInfo = getRestInfoFromBody(e)
            if (StringUtils.isNotBlank(restInfo)) {
                return RestQueryException(msg, e, restInfo)
            }
        }
        return RestQueryException(msg, e)
    }

    private fun getRestInfoFromBody(ex: HttpServerErrorException): String? {
        val body = ex.responseBodyAsString
        val mapper = ObjectMapper()
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        try {
            val bodyMessage = mapper.readValue(body, HttpErrorBody::class.java)
            if (StringUtils.isNotBlank(bodyMessage.message)) {
                return bodyMessage.message
            }
        } catch (ignored: Exception) {
            // do nothing
        }
        return null
    }

    data class HttpErrorBody(
        val message: String? = null
    )
}
