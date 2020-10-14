package ru.citeck.ecos.records2;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.ExceptionUtils;
import ru.citeck.ecos.commons.utils.LibsUtils;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorServiceImpl;
import ru.citeck.ecos.records2.evaluator.evaluators.*;
import ru.citeck.ecos.records2.graphql.RecordsMetaGql;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValuesConverter;
import ru.citeck.ecos.records2.graphql.meta.value.factory.DataValueMetaFactory;
import ru.citeck.ecos.records2.graphql.meta.value.factory.MetaValueFactory;
import ru.citeck.ecos.records2.graphql.meta.value.factory.RecordMetaValueFactory;
import ru.citeck.ecos.records2.graphql.types.GqlMetaQueryDef;
import ru.citeck.ecos.records2.graphql.types.GqlTypeDefinition;
import ru.citeck.ecos.records2.graphql.types.MetaEdgeTypeDef;
import ru.citeck.ecos.records2.graphql.types.MetaValueTypeDef;
import ru.citeck.ecos.records2.meta.AttributesMetaResolver;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.meta.RecordsMetaServiceImpl;
import ru.citeck.ecos.records2.querylang.QueryLangService;
import ru.citeck.ecos.records2.querylang.QueryLangServiceImpl;
import ru.citeck.ecos.records2.request.rest.RestHandler;
import ru.citeck.ecos.records3.RecordsServiceImpl;
import ru.citeck.ecos.records3.record.op.atts.schema.write.AttSchemaGqlWriter;
import ru.citeck.ecos.records3.record.op.atts.schema.write.AttSchemaWriter;
import ru.citeck.ecos.records3.record.op.atts.value.AttValuesConverter;
import ru.citeck.ecos.records3.record.op.atts.value.factory.*;
import ru.citeck.ecos.records3.record.op.atts.value.factory.bean.BeanValueFactory;
import ru.citeck.ecos.records3.record.op.atts.service.RecordAttsService;
import ru.citeck.ecos.records3.record.op.atts.service.RecordAttsServiceImpl;
import ru.citeck.ecos.records3.record.op.atts.proc.*;
import ru.citeck.ecos.records3.record.op.atts.schema.read.AttSchemaReader;
import ru.citeck.ecos.records3.record.op.atts.schema.resolver.AttSchemaResolver;
import ru.citeck.ecos.records3.rest.RestHandlerAdapter;
import ru.citeck.ecos.records2.meta.RecordsTemplateService;
import ru.citeck.ecos.records3.record.op.atts.schema.read.DtoSchemaReader;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.PredicateServiceImpl;
import ru.citeck.ecos.records2.predicate.api.records.PredicateRecords;
import ru.citeck.ecos.records2.predicate.json.std.PredicateJsonDeserializer;
import ru.citeck.ecos.records2.predicate.json.std.PredicateJsonSerializer;
import ru.citeck.ecos.records2.predicate.json.std.PredicateTypes;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.record.resolver.LocalRecordsResolver;
import ru.citeck.ecos.records3.record.resolver.LocalRemoteResolver;
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver;
import ru.citeck.ecos.records2.source.common.group.RecordsGroupDao;
import ru.citeck.ecos.records3.record.dao.RecordsDao;
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDaoAttsProvider;
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDaoAttsProviderImpl;
import ru.citeck.ecos.records2.source.dao.local.source.RecordsSourceRecordsDao;
import ru.citeck.ecos.records2.type.DefaultRecTypeService;
import ru.citeck.ecos.records2.type.RecordTypeService;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

@Slf4j
public class RecordsServiceFactory {

    private AttributesMetaResolver attributesMetaResolver;
    private RestHandlerAdapter restHandlerAdapter;
    private RecordsMetaGql recordsMetaGql;
    private RestHandler restHandler;
    private RecordsService recordsService;
    private ru.citeck.ecos.records3.RecordsService recordsServiceV1;
    private DtoSchemaReader dtoSchemaReader;
    private LocalRemoteResolver recordsResolver;
    private PredicateService predicateService;
    private QueryLangService queryLangService;
    private RecordsMetaService recordsMetaService;
    private RecordAttsService recordsAttsService;
    private LocalRecordsResolver localRecordsResolver;
    private RemoteRecordsResolver remoteRecordsResolver;
    private AttValuesConverter attValuesConverter;
    private RecordEvaluatorService recordEvaluatorService;
    private PredicateJsonDeserializer predicateJsonDeserializer;
    private PredicateTypes predicateTypes;
    private List<RecordsDao> defaultRecordsDao;
    private RecordTypeService recordTypeService;
    private RecordsTemplateService recordsTemplateService;
    private AttProcService attProcService;
    private AttSchemaReader attSchemaReader;
    private AttSchemaWriter attSchemaWriter;
    private AttSchemaResolver attSchemaResolver;
    private MetaValuesConverter metaValuesConverter;

    @Deprecated
    private Supplier<? extends QueryContext> queryContextSupplier;
    private Supplier<? extends RequestContext> requestContextSupplier;

    @Deprecated
    private List<MetaValueFactory<?>> metaValueFactories;
    private List<AttValueFactory<?>> attValueFactories;

    private MetaRecordsDaoAttsProvider metaRecordsDaoAttsProvider;

    private RecordsProperties properties;

    private RecordEvaluatorService tmpEvaluatorsService;

    private ru.citeck.ecos.records3.RecordsService tmpRecordsService;
    private List<GqlTypeDefinition> gqlTypes;

    private boolean isJobsInitialized = false;

    {
        Json.getContext().addDeserializer(getPredicateJsonDeserializer());
        Json.getContext().addSerializer(new PredicateJsonSerializer());
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

    public final synchronized AttributesMetaResolver getAttributesMetaResolver() {
        if (attributesMetaResolver == null) {
            attributesMetaResolver = createAttributesMetaResolver();
        }
        return attributesMetaResolver;
    }

    protected AttributesMetaResolver createAttributesMetaResolver() {
        return new AttributesMetaResolver();
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

        gqlTypes.add(new GqlMetaQueryDef(this));
        gqlTypes.add(new MetaEdgeTypeDef(metaValueTypeDef));

        return gqlTypes;
    }

    public synchronized void initJobs(ScheduledExecutorService executor) {
        if (!isJobsInitialized) {
            log.info("Records jobs initialization started. Executor: " + executor);
            getLocalRecordsResolver().initJobs(executor);
            isJobsInitialized = true;
        }
    }

    public final synchronized PredicateTypes getPredicateTypes() {
        if (predicateTypes == null) {
            predicateTypes = createPredicateTypes();
        }
        return predicateTypes;
    }

    protected PredicateTypes createPredicateTypes() {
        return new PredicateTypes();
    }

    public final synchronized PredicateJsonDeserializer getPredicateJsonDeserializer() {
        if (predicateJsonDeserializer == null) {
            predicateJsonDeserializer = createPredicateJsonDeserializer();
        }
        return predicateJsonDeserializer;
    }

    protected synchronized PredicateJsonDeserializer createPredicateJsonDeserializer() {
        return new PredicateJsonDeserializer(getPredicateTypes());
    }

    public final synchronized RecordEvaluatorService getRecordEvaluatorService() {
        if (recordEvaluatorService == null) {
            if (tmpEvaluatorsService != null) {
                recordEvaluatorService = tmpEvaluatorsService;
            } else {
                recordEvaluatorService = createRecordEvaluatorService();
            }
        }
        return recordEvaluatorService;
    }

    protected RecordEvaluatorService createRecordEvaluatorService() {

        if (tmpEvaluatorsService != null) {
            return tmpEvaluatorsService;
        }

        RecordEvaluatorService service = new RecordEvaluatorServiceImpl(this);
        tmpEvaluatorsService = service;

        service.register(new GroupEvaluator());
        service.register(new PredicateEvaluator());
        service.register(new AlwaysTrueEvaluator());
        service.register(new AlwaysFalseEvaluator());
        service.register(new HasAttributeEvaluator());
        service.register(new HasPermissionEvaluator());

        tmpEvaluatorsService = null;

        return service;
    }

    public final synchronized RecordsService getRecordsService() {
        if (recordsService == null) {
            recordsService = createRecordsService();
        }
        return recordsService;
    }

    protected RecordsService createRecordsService() {
        return new ru.citeck.ecos.records2.RecordsServiceImpl(this);
    }

    public final synchronized ru.citeck.ecos.records3.RecordsService getRecordsServiceV1() {
        if (recordsServiceV1 == null) {
            recordsServiceV1 = createRecordsServiceV1();
        }
        return recordsServiceV1;
    }

    protected ru.citeck.ecos.records3.RecordsService createRecordsServiceV1() {

        if (tmpRecordsService != null) {
            return tmpRecordsService;
        }

        ru.citeck.ecos.records3.RecordsService recordsService;
        Class<? extends ru.citeck.ecos.records3.RecordsService> serviceType = getRecordsServiceType();
        try {
            Constructor<? extends ru.citeck.ecos.records3.RecordsService> constructor = serviceType.getConstructor(RecordsServiceFactory.class);
            recordsService = constructor.newInstance(this);
        } catch (Exception e) {
            ExceptionUtils.throwException(e);
            throw new RuntimeException(e);
        }

        tmpRecordsService = recordsService;

        getDefaultRecordsDao().forEach(recordsService::register);

        tmpRecordsService = null;
        return recordsService;
    }

    protected Class<? extends ru.citeck.ecos.records3.RecordsService> getRecordsServiceType() {
        return RecordsServiceImpl.class;
    }

    protected List<RecordsDao> getDefaultRecordsDao() {
        if (defaultRecordsDao == null) {
            defaultRecordsDao = Arrays.asList(
                new MetaRecordsDao(this),
                new RecordsSourceRecordsDao(),
                new PredicateRecords()
            );
        }
        return defaultRecordsDao;
    }

    public final synchronized LocalRemoteResolver getRecordsResolver() {
        if (recordsResolver == null) {
            recordsResolver = createRecordsResolver();
        }
        return recordsResolver;
    }

    protected LocalRemoteResolver createRecordsResolver() {
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
            createDefaultRecordsDao().forEach(dao -> localRecordsResolver.register(dao.getId(), dao));
        }
        return localRecordsResolver;
    }

    protected LocalRecordsResolver createLocalRecordsResolver() {
        return new LocalRecordsResolver(this);
    }

    protected List<RecordsDao> createDefaultRecordsDao() {
        return Collections.singletonList(new RecordsGroupDao());
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

    public final synchronized RecordAttsService getRecordsAttsService() {
        if (recordsAttsService == null) {
            recordsAttsService = createRecordsAttsService();
        }
        return recordsAttsService;
    }

    protected RecordAttsService createRecordsAttsService() {
        return new RecordAttsServiceImpl(this);
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

    public final synchronized AttValuesConverter getAttValuesConverter() {
        if (attValuesConverter == null) {
            attValuesConverter = createAttValuesConverter();
        }
        return attValuesConverter;
    }

    protected AttValuesConverter createAttValuesConverter() {
        return new AttValuesConverter(this);
    }

    public final synchronized List<AttValueFactory<?>> getAttValueFactories() {
        if (attValueFactories == null) {
            attValueFactories = createAttValueFactories();
        }
        return attValueFactories;
    }

    protected List<AttValueFactory<?>> createAttValueFactories() {

        List<AttValueFactory<?>> metaValueFactories = new ArrayList<>();

        metaValueFactories.add(new ObjectDataValueFactory());
        metaValueFactories.add(new ByteArrayValueFactory());
        metaValueFactories.add(new DataValueAttFactory());
        metaValueFactories.add(new MLTextValueFactory());
        metaValueFactories.add(new RecordAttValueFactory());
        metaValueFactories.add(new BeanValueFactory());
        metaValueFactories.add(new BooleanValueFactory());
        metaValueFactories.add(new DateValueFactory());
        metaValueFactories.add(new DoubleValueFactory());
        metaValueFactories.add(new IntegerValueFactory());
        metaValueFactories.add(new JsonNodeValueFactory());
        metaValueFactories.add(new LongValueFactory());
        metaValueFactories.add(new StringValueFactory());
        metaValueFactories.add(new RecordRefValueFactory(this));

        if (LibsUtils.isJacksonPresent()) {
            metaValueFactories.add(new JacksonJsonNodeValueFactory());
        }

        return metaValueFactories;
    }
    public final synchronized List<MetaValueFactory<?>> getMetaValueFactories() {
        if (metaValueFactories == null) {
            metaValueFactories = createMetaValueFactories();
        }
        return metaValueFactories;
    }

    protected List<MetaValueFactory<?>> createMetaValueFactories() {

        List<MetaValueFactory<?>> metaValueFactories = new ArrayList<>();

        metaValueFactories.add(new ru.citeck.ecos.records2.graphql.meta.value.factory.ObjectDataValueFactory());
        metaValueFactories.add(new ru.citeck.ecos.records2.graphql.meta.value.factory.ByteArrayValueFactory());
        metaValueFactories.add(new DataValueMetaFactory());
        metaValueFactories.add(new ru.citeck.ecos.records2.graphql.meta.value.factory.MLTextValueFactory());
        metaValueFactories.add(new RecordMetaValueFactory());
        metaValueFactories.add(new ru.citeck.ecos.records2.graphql.meta.value.factory.BeanValueFactory());
        metaValueFactories.add(new ru.citeck.ecos.records2.graphql.meta.value.factory.BooleanValueFactory());
        metaValueFactories.add(new ru.citeck.ecos.records2.graphql.meta.value.factory.DateValueFactory());
        metaValueFactories.add(new ru.citeck.ecos.records2.graphql.meta.value.factory.DoubleValueFactory());
        metaValueFactories.add(new ru.citeck.ecos.records2.graphql.meta.value.factory.IntegerValueFactory());
        metaValueFactories.add(new ru.citeck.ecos.records2.graphql.meta.value.factory.JsonNodeValueFactory());
        metaValueFactories.add(new ru.citeck.ecos.records2.graphql.meta.value.factory.LongValueFactory());
        metaValueFactories.add(new ru.citeck.ecos.records2.graphql.meta.value.factory.StringValueFactory());
        metaValueFactories.add(new ru.citeck.ecos.records2.graphql.meta.value.factory.RecordRefValueFactory(this));

        if (LibsUtils.isJacksonPresent()) {
            metaValueFactories.add(new ru.citeck.ecos.records2.graphql.meta.value.factory.JacksonJsonNodeValueFactory());
        }

        return metaValueFactories;
    }

    public final synchronized DtoSchemaReader getDtoSchemaReader() {
        if (dtoSchemaReader == null) {
            dtoSchemaReader = createDtoSchemaReader();
        }
        return dtoSchemaReader;
    }

    protected DtoSchemaReader createDtoSchemaReader() {
        return new DtoSchemaReader(this);
    }

    public final synchronized Supplier<? extends RequestContext> getRequestContextSupplier() {
        if (requestContextSupplier == null) {
            requestContextSupplier = createRequestContextSupplier();
        }
        return requestContextSupplier;
    }

    protected Supplier<? extends RequestContext> createRequestContextSupplier() {
        return RequestContext::new;
    }

    public final synchronized RequestContext createRequestContext() {
        RequestContext context = getRequestContextSupplier().get();
        context.setServiceFactory(this);
        return context;
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

    protected RestHandlerAdapter createRestHandlerAdapter() {
        return new RestHandlerAdapter(this);
    }

    public final synchronized RestHandlerAdapter getRestHandlerAdapter() {
        if (restHandlerAdapter == null) {
            restHandlerAdapter = createRestHandlerAdapter();
        }
        return restHandlerAdapter;
    }

    protected RecordsProperties createProperties() {
        return new RecordsProperties();
    }

    public final synchronized RecordsProperties getProperties() {
        if (properties == null) {
            properties = createProperties();
        }
        return properties;
    }

    protected MetaRecordsDaoAttsProvider createMetaRecordsDaoAttsProvider() {
        return new MetaRecordsDaoAttsProviderImpl();
    }

    public final synchronized MetaRecordsDaoAttsProvider getMetaRecordsDaoAttsProvider() {
        if (metaRecordsDaoAttsProvider == null) {
            metaRecordsDaoAttsProvider = createMetaRecordsDaoAttsProvider();
        }
        return metaRecordsDaoAttsProvider;
    }

    protected RecordTypeService createRecordTypeService() {
        return new DefaultRecTypeService(this);
    }

    public final synchronized RecordTypeService getRecordTypeService() {
        if (recordTypeService == null) {
            recordTypeService = createRecordTypeService();
        }
        return recordTypeService;
    }

    protected RecordsTemplateService createRecordsTemplateService() {
        return new RecordsTemplateService(this);
    }

    public final synchronized RecordsTemplateService getRecordsTemplateService() {
        if (recordsTemplateService == null) {
            recordsTemplateService = createRecordsTemplateService();
        }
        return recordsTemplateService;
    }

    protected AttSchemaReader createAttSchemaReader() {
        return new AttSchemaReader();
    }

    public final synchronized AttSchemaReader getAttSchemaReader() {
        if (attSchemaReader == null) {
            attSchemaReader = createAttSchemaReader();
        }
        return attSchemaReader;
    }

    protected AttSchemaWriter createAttSchemaWriter() {
        return new AttSchemaGqlWriter();
    }

    public final synchronized AttSchemaWriter getAttSchemaWriter() {
        if (attSchemaWriter == null) {
            attSchemaWriter = createAttSchemaWriter();
        }
        return attSchemaWriter;
    }

    protected AttSchemaResolver createAttSchemaResolver() {
        return new AttSchemaResolver(this);
    }

    public final synchronized AttSchemaResolver getAttSchemaResolver() {
        if (attSchemaResolver == null) {
            attSchemaResolver = createAttSchemaResolver();
        }
        return attSchemaResolver;
    }

    protected List<AttProcessor> getAttProcessors() {
        return Arrays.asList(
            new AttFormatProcessor(),
            new AttPrefixSuffixProcessor(),
            new AttOrElseProcessor(),
            new AttJoinProcessor(),
            new AttCastProcessor()
        );
    }

    protected AttProcService createAttProcService() {
        AttProcService service = new AttProcService();
        getAttProcessors().forEach(service::register);
        return service;
    }

    public final synchronized AttProcService getAttProcService() {
        if (attProcService == null) {
            attProcService = createAttProcService();
        }
        return attProcService;
    }
}
