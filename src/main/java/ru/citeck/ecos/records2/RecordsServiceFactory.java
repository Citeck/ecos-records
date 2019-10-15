package ru.citeck.ecos.records2;

import ru.citeck.ecos.predicate.PredicateService;
import ru.citeck.ecos.predicate.PredicateServiceImpl;
import ru.citeck.ecos.querylang.QueryLangService;
import ru.citeck.ecos.querylang.QueryLangServiceImpl;
import ru.citeck.ecos.records2.graphql.RecordsMetaGql;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValuesConverter;
import ru.citeck.ecos.records2.graphql.meta.value.factory.*;
import ru.citeck.ecos.records2.graphql.types.GqlMetaQueryDef;
import ru.citeck.ecos.records2.graphql.types.GqlTypeDefinition;
import ru.citeck.ecos.records2.graphql.types.MetaEdgeTypeDef;
import ru.citeck.ecos.records2.graphql.types.MetaValueTypeDef;
import ru.citeck.ecos.records2.meta.AttributesMetaResolver;
import ru.citeck.ecos.records2.meta.DtoMetaResolver;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.meta.RecordsMetaServiceImpl;
import ru.citeck.ecos.records2.request.rest.RestHandler;
import ru.citeck.ecos.records2.resolver.LocalRecordsResolver;
import ru.citeck.ecos.records2.resolver.LocalRemoteResolver;
import ru.citeck.ecos.records2.resolver.RecordsResolver;
import ru.citeck.ecos.records2.resolver.RemoteRecordsResolver;
import ru.citeck.ecos.records2.source.common.group.RecordsGroupDAO;
import ru.citeck.ecos.records2.source.dao.RecordsDAO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class RecordsServiceFactory {

    private RestHandler restHandler;
    private RecordsMetaGql recordsMetaGql;
    private RecordsService recordsService;
    private DtoMetaResolver dtoMetaResolver;
    private RecordsResolver recordsResolver;
    private PredicateService predicateService;
    private QueryLangService queryLangService;
    private RecordsMetaService recordsMetaService;
    private List<MetaValueFactory> metaValueFactories;
    private LocalRecordsResolver localRecordsResolver;
    private RemoteRecordsResolver remoteRecordsResolver;
    private AttributesMetaResolver attributesMetaResolver;
    private Supplier<? extends QueryContext> queryContextSupplier;
    private MetaValuesConverter metaValuesConverter;

    private List<GqlTypeDefinition> gqlTypes;

    public final synchronized RecordsService getRecordsService() {
        if (recordsService == null) {
            recordsService = createRecordsService();
        }
        return recordsService;
    }

    protected RecordsService createRecordsService() {
        return new RecordsServiceImpl(this);
    }

    public final synchronized RecordsResolver getRecordsResolver() {
        if (recordsResolver == null) {
            recordsResolver = createRecordsResolver();
        }
        return recordsResolver;
    }

    protected RecordsResolver createRecordsResolver() {
        return new LocalRemoteResolver(this);
    }

    public final synchronized RemoteRecordsResolver getRemoteRecordsResolver() {
        if (remoteRecordsResolver == null) {
            remoteRecordsResolver = createRemoteRecordsResolver();
        }
        return remoteRecordsResolver;
    }

    protected RemoteRecordsResolver createRemoteRecordsResolver() {
        return null;
    }

    public final synchronized LocalRecordsResolver getLocalRecordsResolver() {
        if (localRecordsResolver == null) {
            localRecordsResolver = createLocalRecordsResolver();
            createDefaultRecordsDAO().forEach(localRecordsResolver::register);
        }
        return localRecordsResolver;
    }

    protected LocalRecordsResolver createLocalRecordsResolver() {
        return new LocalRecordsResolver(this);
    }

    protected List<RecordsDAO> createDefaultRecordsDAO() {
        return Collections.singletonList(new RecordsGroupDAO());
    }

    public final synchronized QueryLangService getQueryLangService() {
        if (queryLangService == null) {
            queryLangService = createQueryLangService();
        }
        return queryLangService;
    }

    protected QueryLangService createQueryLangService() {
        return new QueryLangServiceImpl();
    }

    public final synchronized RecordsMetaService getRecordsMetaService() {
        if (recordsMetaService == null) {
            recordsMetaService = createRecordsMetaService();
        }
        return recordsMetaService;
    }

    protected RecordsMetaService createRecordsMetaService() {
        return new RecordsMetaServiceImpl(this);
    }

    public final synchronized PredicateService getPredicateService() {
        if (predicateService == null) {
            predicateService = createPredicateService();
        }
        return predicateService;
    }

    protected PredicateService createPredicateService() {
        return new PredicateServiceImpl();
    }

    public final synchronized RecordsMetaGql getRecordsMetaGql() {
        if (recordsMetaGql == null) {
            recordsMetaGql = createRecordsMetaGql();
        }
        return recordsMetaGql;
    }

    protected RecordsMetaGql createRecordsMetaGql() {
        return new RecordsMetaGql(this);
    }

    public final synchronized List<GqlTypeDefinition> getGqlTypes() {
        if (gqlTypes == null) {
            gqlTypes = createGqlTypes();
        }
        return gqlTypes;
    }

    protected List<GqlTypeDefinition> createGqlTypes() {

        List<GqlTypeDefinition> gqlTypes = new ArrayList<>();
        MetaValueTypeDef metaValueTypeDef = new MetaValueTypeDef(this);

        gqlTypes.add(metaValueTypeDef);

        GqlMetaQueryDef gqlMetaQueryDef = new GqlMetaQueryDef();
        gqlMetaQueryDef.setMetaValueTypeDef(metaValueTypeDef);

        gqlTypes.add(gqlMetaQueryDef);
        gqlTypes.add(new MetaEdgeTypeDef(metaValueTypeDef));

        return gqlTypes;
    }

    public final synchronized MetaValuesConverter getMetaValuesConverter() {
        if (metaValuesConverter == null) {
            metaValuesConverter = createMetaValuesConverter();
        }
        return metaValuesConverter;
    }

    protected MetaValuesConverter createMetaValuesConverter() {
        return new MetaValuesConverter(this);
    }

    public final synchronized List<MetaValueFactory> getMetaValueFactories() {
        if (metaValueFactories == null) {
            metaValueFactories = createMetaValueFactories();
        }
        return metaValueFactories;
    }

    protected List<MetaValueFactory> createMetaValueFactories() {

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
        metaValueFactories.add(new RecordRefValueFactory(this));

        return metaValueFactories;
    }

    public final synchronized AttributesMetaResolver getAttributesMetaResolver() {
        if (attributesMetaResolver == null) {
            attributesMetaResolver = createAttributesMetaResolver();
        }
        return attributesMetaResolver;
    }

    protected AttributesMetaResolver createAttributesMetaResolver() {
        return new AttributesMetaResolver();
    }

    public final synchronized DtoMetaResolver getDtoMetaResolver() {
        if (dtoMetaResolver == null) {
            dtoMetaResolver = createDtoMetaResolver();
        }
        return dtoMetaResolver;
    }

    protected DtoMetaResolver createDtoMetaResolver() {
        return new DtoMetaResolver(this);
    }

    public final synchronized Supplier<? extends QueryContext> getQueryContextSupplier() {
        if (queryContextSupplier == null) {
            queryContextSupplier = createQueryContextSupplier();
        }
        return queryContextSupplier;
    }

    protected Supplier<? extends QueryContext> createQueryContextSupplier() {
        return QueryContext::new;
    }

    public final synchronized QueryContext createQueryContext() {
        QueryContext context = getQueryContextSupplier().get();
        context.setServiceFactory(this);
        return context;
    }

    protected RestHandler createRestHandler() {
        return new RestHandler(this);
    }

    public final synchronized RestHandler getRestHandler() {
        if (restHandler == null) {
            restHandler = createRestHandler();
        }
        return restHandler;
    }
}
