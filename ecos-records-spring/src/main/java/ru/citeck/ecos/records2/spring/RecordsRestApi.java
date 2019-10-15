package ru.citeck.ecos.records2.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.citeck.ecos.records2.request.rest.DeletionBody;
import ru.citeck.ecos.records2.request.rest.MutationBody;
import ru.citeck.ecos.records2.request.rest.QueryBody;
import ru.citeck.ecos.records2.request.rest.RestHandler;

@RestController
@RequestMapping("/api/records")
public class RecordsRestApi {

    private RestHandler restHandler;

    @Autowired
    public RecordsRestApi(RestHandler restHandler) {
        this.restHandler = restHandler;
    }

    @PostMapping("/query")
    public Object recordsQuery(@RequestBody QueryBody body) {
        return restHandler.queryRecords(body);
    }

    @PostMapping("/mutate")
    public Object recordsMutate(@RequestBody MutationBody body) {
        return restHandler.mutateRecords(body);
    }

    @PostMapping("/delete")
    public Object recordsDelete(@RequestBody DeletionBody body) {
        return restHandler.deleteRecords(body);
    }
}

