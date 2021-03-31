package ru.citeck.ecos.records2.graphql;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordsService;

/**
 * Query context.
 * @deprecated use RequestContext instead
 */
@Slf4j
@Deprecated
public class GqlContext extends QueryContext {

    public GqlContext() {
    }

    public GqlContext(RecordsService recordsService) {
        log.warn("GQL Context is deprecated! " + recordsService);
    }
}
