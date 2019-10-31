package ru.citeck.ecos.records2.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.citeck.ecos.records2.request.rest.DeletionBody;
import ru.citeck.ecos.records2.request.rest.MutationBody;
import ru.citeck.ecos.records2.request.rest.QueryBody;
import ru.citeck.ecos.records2.request.rest.RestHandler;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.utils.SecurityUtils;

@RestController
@RequestMapping("/api/records")
public class RecordsRestApi {

    private RestHandler restHandler;
    private Environment environment;

    private boolean isProdProfile = true;

    @Autowired
    public RecordsRestApi(RestHandler restHandler, Environment environment) {
        this.restHandler = restHandler;
        this.environment = environment;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        isProdProfile = environment.acceptsProfiles("prod");
    }

    @PostMapping("/query")
    public Object recordsQuery(@RequestBody QueryBody body) {
        return encodeResponse(restHandler.queryRecords(body));
    }

    @PostMapping("/mutate")
    public Object recordsMutate(@RequestBody MutationBody body) {
        return encodeResponse(restHandler.mutateRecords(body));
    }

    @PostMapping("/delete")
    public Object recordsDelete(@RequestBody DeletionBody body) {
        return encodeResponse(restHandler.deleteRecords(body));
    }

    private Object encodeResponse(Object response) {
        if (!isProdProfile || !(response instanceof RecordsResult)) {
            return response;
        }
        return SecurityUtils.encodeResult((RecordsResult<?>) response);
    }
}

