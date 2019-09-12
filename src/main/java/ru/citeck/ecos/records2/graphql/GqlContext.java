package ru.citeck.ecos.records2.graphql;

import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordsServiceFactory;

/**
 * Query context.
 * @deprecated use QueryContext instead
 */
@Deprecated
public class GqlContext extends QueryContext {

    public GqlContext(RecordsServiceFactory serviceFactory) {
        super(serviceFactory);
    }
}
