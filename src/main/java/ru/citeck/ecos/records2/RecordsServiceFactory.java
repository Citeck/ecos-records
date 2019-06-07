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
import ru.citeck.ecos.records2.source.common.group.RecordsGroupDAO;

import java.util.ArrayList;
import java.util.List;

public class RecordsServiceFactory {

    private RecordsService recordsService;
    private RecordsResolver recordsResolver;
    private PredicateService predicateService;
    private QueryLangService queryLangService;

    public RecordsService createRecordsService() {
        recordsService = new RecordsServiceImpl(createRecordsMetaService(), createRecordsResolver());
        return recordsService;
    }

    public RecordsResolver createRecordsResolver() {
        LocalRecordsResolver resolver = new LocalRecordsResolver(createQueryLangService());
        resolver.setPredicateService(createPredicateService());
        resolver.register(new RecordsGroupDAO());
        this.recordsResolver = resolver;
        return resolver;
    }

    public QueryLangService createQueryLangService() {
        queryLangService = new QueryLangServiceImpl();
        return queryLangService;
    }

    public RecordsMetaService createRecordsMetaService() {
        return new RecordsMetaServiceImpl(createRecordsMetaGraphQL());
    }

    public PredicateService createPredicateService() {
        predicateService = new PredicateServiceImpl();
        return predicateService;
    }

    public RecordsMetaGql createRecordsMetaGraphQL() {
        return new RecordsMetaGql(getGqlTypes(), () -> new GqlContext(recordsService));
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
