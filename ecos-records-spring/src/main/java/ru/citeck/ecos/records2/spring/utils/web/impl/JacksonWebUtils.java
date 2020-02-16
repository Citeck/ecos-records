package ru.citeck.ecos.records2.spring.utils.web.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.spring.utils.web.WebUtils;
import ru.citeck.ecos.records2.spring.utils.web.exception.RequestHandlingException;
import ru.citeck.ecos.records2.spring.utils.web.exception.ResponseHandlingException;
import ru.citeck.ecos.records2.utils.SecurityUtils;
import ru.citeck.ecos.records2.utils.json.JsonUtils;

/**
 * WebUtils implementation for work with request and response with Jackson lib.
 */
@Component
@Slf4j
public class JacksonWebUtils implements WebUtils, EnvironmentAware {

    private boolean isProdProfile = true;
    private Environment environment;

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        isProdProfile = environment != null && environment.acceptsProfiles("prod");
    }

    public <T> T convertRequest(byte[] body, Class<T> valueType) {
        try {
            return JsonUtils.read(body, valueType);
        } catch (Exception ioe) {
            log.error("Jackson cannot parse request body", ioe);
            throw new RequestHandlingException(ioe);
        }
    }

    public byte[] encodeResponse(Object response) {
        try {
            if (isProdProfile && response instanceof RecordsResult) {
                SecurityUtils.encodeResult((RecordsResult<?>) response);
            }
            return JsonUtils.toBytes(response);
        } catch (Exception jpe) {
            log.error("Jackson cannot write response body as bytes", jpe);
            throw new ResponseHandlingException(jpe);
        }
    }

    @Autowired(required = false)
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}