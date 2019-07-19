package ru.citeck.ecos.records2;

import ru.citeck.ecos.predicate.PredicateService;
import ru.citeck.ecos.predicate.PredicateServiceImpl;
import ru.citeck.ecos.querylang.QueryLangService;
import ru.citeck.ecos.querylang.QueryLangServiceImpl;
import ru.citeck.ecos.records2.graphql.GqlContext;
import ru.citeck.ecos.records2.graphql.RecordsMetaGql;
import ru.citeck.ecos.records2.graphql.meta.value.factory.*;
import ru.citeck.ecos.records2.graphql.types.GqlMetaQueryDef;
import ru.citeck.ecos.records2.graphql.types.GqlTypeDefinition;
import ru.citeck.ecos.records2.graphql.types.MetaEdgeTypeDef;
import ru.citeck.ecos.records2.graphql.types.MetaValueTypeDef;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.meta.RecordsMetaServiceImpl;
import ru.citeck.ecos.records2.resolver.RecordsResolver;
import ru.citeck.ecos.records2.resolver.LocalRecordsResolver;
import ru.citeck.ecos.records2.resolver.LocalRemoteResolver;
import ru.citeck.ecos.records2.resolver.RemoteRecordsResolver;
import ru.citeck.ecos.records2.source.common.group.RecordsGroupDAO;

import java.util.ArrayList;
import java.util.List;

public class RecordsServiceFactory {

    protected RecordsService recordsService;
    protected RecordsResolver recordsResolver;
    protected PredicateService predicateService;
    protected QueryLangService queryLangService;
    protected RecordsMetaGql recordsMetaGql;
    protected RecordsMetaService recordsMetaService;

    private LocalRecordsResolver localRecordsResolver;

    public RecordsService createRecordsService() {
        recordsService = new RecordsServiceImpl(createRecordsMetaService(), createRecordsResolver());
        return recordsService;
    }

    public RecordsResolver createRecordsResolver() {
        if (recordsResolver == null) {
            this.recordsResolver = new LocalRemoteResolver(createLocalRecordsResolver(),
                                                           createRemoteRecordsResolver());
        }
        return recordsResolver;
    }

    protected RemoteRecordsResolver createRemoteRecordsResolver() {
        return null;
    }

    protected LocalRecordsResolver createLocalRecordsResolver() {

        if (localRecordsResolver == null) {
            LocalRecordsResolver resolver = new LocalRecordsResolver(createQueryLangService());
            resolver.setPredicateService(createPredicateService());
            resolver.register(new RecordsGroupDAO());
            localRecordsResolver = resolver;
        }

        return localRecordsResolver;
    }

    public QueryLangService createQueryLangService() {
        if (queryLangService == null) {
            queryLangService = new QueryLangServiceImpl();
        }
        return queryLangService;
    }

    public RecordsMetaService createRecordsMetaService() {
        if (recordsMetaService == null) {
            recordsMetaService = new RecordsMetaServiceImpl(createRecordsMetaGraphQL());
        }
        return recordsMetaService;
    }

    public PredicateService createPredicateService() {
        if (predicateService == null) {
            predicateService = new PredicateServiceImpl();
        }
        return predicateService;
    }

    public RecordsMetaGql createRecordsMetaGraphQL() {
        if (recordsMetaGql == null) {
            recordsMetaGql = new RecordsMetaGql(getGqlTypes(), () -> new GqlContext(recordsService));
        }
        return recordsMetaGql;
    }

    protected List<GqlTypeDefinition> getGqlTypes() {

        List<GqlTypeDefinition> types = new ArrayList<>();

        MetaValueTypeDef metaValueTypeDef = new MetaValueTypeDef();
        metaValueTypeDef.setMetaValueFactories(getMetaValueFactories());

        types.add(metaValueTypeDef);

        GqlMetaQueryDef gqlMetaQueryDef = new GqlMetaQueryDef();
        gqlMetaQueryDef.setMetaValueTypeDef(metaValueTypeDef);

        types.add(gqlMetaQueryDef);
        types.add(new MetaEdgeTypeDef(metaValueTypeDef));

        return types;
    }

    protected List<MetaValueFactory> getMetaValueFactories() {

        List<MetaValueFactory> metaValueFactories = new ArrayList<>();

        metaValueFactories.add(new RecordMetaValueFactory());
        metaValueFactories.add(new BeanValueFactory());
        metaValueFactories.add(new BooleanValueFactory());
        metaValueFactories.add(new DateValueFactory());
        metaValueFactories.add(new DoubleValueFactory());
        metaValueFactories.add(new IntegerValueFactory());
        metaValueFactories.add(new JsonNodeValueFactory());
        metaValueFactories.add(new LongValueFactory());
        metaValueFactories.add(new StringValueFactory());
        metaValueFactories.add(new RecordRefValueFactory());

        return metaValueFactories;
    }

    public RecordsService getRecordsService() {
        return recordsService;
    }

    public RecordsResolver getRecordsResolver() {
        return recordsResolver;
    }

    public PredicateService getPredicateService() {
        return predicateService;
    }

    public QueryLangService getQueryLangService() {
        return queryLangService;
    }
}
