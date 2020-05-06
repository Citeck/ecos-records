package ru.citeck.ecos.records2.spring.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import ru.citeck.ecos.records2.rest.RestQueryException;
import ru.citeck.ecos.records2.rest.RestQueryExceptionConverter;

@Component
public class RestQueryExceptionConverterImpl implements RestQueryExceptionConverter {

    @Override
    public RestQueryException convert(String msg, Exception e) {
        if (e == null) {
            return new RestQueryException(msg);
        }

        if (e instanceof HttpServerErrorException) {
            String restInfo = getRestInfoFromBody((HttpServerErrorException) e);
            if (StringUtils.isNotBlank(restInfo)) {
                return new RestQueryException(msg, e, restInfo);
            }
        }

        return new RestQueryException(msg, e);
    }

    private String getRestInfoFromBody(HttpServerErrorException ex) {
        String body = ex.getResponseBodyAsString();

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        try {
            HttpErrorBody bodyMessage = mapper.readValue(body, HttpErrorBody.class);
            if (StringUtils.isNotBlank(bodyMessage.getMessage())) {
                return bodyMessage.getMessage();
            }
        } catch (Exception ignored) {
            //do nothing
        }

        return null;
    }

    @Data
    public static class HttpErrorBody {
        private String message;
    }
}
