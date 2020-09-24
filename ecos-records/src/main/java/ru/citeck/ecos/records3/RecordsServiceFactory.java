package ru.citeck.ecos.records3;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.ExceptionUtils;
import ru.citeck.ecos.commons.utils.LibsUtils;
import ru.citeck.ecos.records3.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.records3.evaluator.RecordEvaluatorServiceImpl;
import ru.citeck.ecos.records3.evaluator.evaluators.*;
import ru.citeck.ecos.records3.graphql.meta.value.MetaValuesConverter;
import ru.citeck.ecos.records3.graphql.meta.value.factory.*;
import ru.citeck.ecos.records3.graphql.meta.value.factory.bean.BeanValueFactory;
import ru.citeck.ecos.records3.record.operation.meta.RecordsMetaService;
import ru.citeck.ecos.records3.record.operation.meta.RecordsMetaServiceImpl;
import ru.citeck.ecos.records3.template.RecordsTemplateService;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.DtoSchemaResolver;
import ru.citeck.ecos.records3.predicate.PredicateService;
import ru.citeck.ecos.records3.predicate.PredicateServiceImpl;
import ru.citeck.ecos.records3.predicate.api.records.PredicateRecords;
import ru.citeck.ecos.records3.predicate.json.std.PredicateJsonDeserializer;
import ru.citeck.ecos.records3.predicate.json.std.PredicateJsonSerializer;
import ru.citeck.ecos.records3.predicate.json.std.PredicateTypes;
import ru.citeck.ecos.records3.record.operation.query.QueryContext;
import ru.citeck.ecos.records3.record.operation.query.lang.QueryLangService;
import ru.citeck.ecos.records3.record.operation.query.lang.QueryLangServiceImpl;
import ru.citeck.ecos.records3.rest.RestHandler;
import ru.citeck.ecos.records3.record.resolver.LocalRecordsResolver;
import ru.citeck.ecos.records3.record.resolver.LocalRemoteResolver;
import ru.citeck.ecos.records3.record.resolver.RecordsResolver;
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver;
import ru.citeck.ecos.records3.source.common.group.RecordsGroupDao;
import ru.citeck.ecos.records3.source.dao.RecordsDao;
import ru.citeck.ecos.records3.source.dao.local.meta.MetaRecordsDao;
import ru.citeck.ecos.records3.source.dao.local.meta.MetaRecordsDaoAttsProvider;
import ru.citeck.ecos.records3.source.dao.local.meta.MetaRecordsDaoAttsProviderImpl;
import ru.citeck.ecos.records3.source.dao.local.source.RecordsSourceRecordsDao;
import ru.citeck.ecos.records3.type.DefaultRecordTypeService;
import ru.citeck.ecos.records3.type.RecordTypeService;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

@Slf4j
public class RecordsServiceFactory {

    private RestHandler restHandler;
    private RecordsService recordsService;
    private DtoSchemaResolver dtoMetaResolver;
    private RecordsResolver recordsResolver;
    private PredicateService predicateService;
    private QueryLangService queryLangService;
    private RecordsMetaService recordsMetaService;
    private List<MetaValueFactory<?>> metaValueFactories;
    private LocalRecordsResolver localRecordsResolver;
    private RemoteRecordsResolver remoteRecordsResolver;
    private AttributesMetaResolver attributesMetaResolver;
    private Supplier<? extends QueryContext> queryContextSupplier;
    private MetaValuesConverter metaValuesConverter;
    private RecordEvaluatorService recordEvaluatorService;
    private PredicateJsonDeserializer predicateJsonDeserializer;
    private PredicateTypes predicateTypes;
    private List<RecordsDao> defaultRecordsDao;
    private RecordTypeService recordTypeService;
    private RecordsTemplateService recordsTemplateService;
    private AttProcService attProcService;

    private MetaRecordsDaoAttsProvider metaRecordsDaoAttsProvider;

    private RecordsProperties properties;

    private RecordEvaluatorService tmpEvaluatorsService;
    private RecordsService tmpRecordsService;

    private boolean isJobsInitialized = false;

    {
        Json.getContext().addDeserializer(getPredicateJsonDeserializer());
        Json.getContext().addSerializer(new PredicateJsonSerializer());
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

        if (tmpRecordsService != null) {
            return tmpRecordsService;
        }

        RecordsService recordsService;
        Class<? extends RecordsService> serviceType = getRecordsServiceType();
        try {
            Constructor<? extends RecordsService> constructor = serviceType.getConstructor(RecordsServiceFactory.class);
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

    protected Class<? extends RecordsService> getRecordsServiceType() {
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

    public final synchronized PredicateService getPredicateService() {
        if (predicateService == null) {
            predicateService = createPredicateService();
        }
        return predicateService;
    }

    protected PredicateService createPredicateService() {
        return new PredicateServiceImpl();
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

    public final synchronized List<MetaValueFactory<?>> getMetaValueFactories() {
        if (metaValueFactories == null) {
            metaValueFactories = createMetaValueFactories();
        }
        return metaValueFactories;
    }

    protected List<MetaValueFactory<?>> createMetaValueFactories() {

        List<MetaValueFactory<?>> metaValueFactories = new ArrayList<>();

        metaValueFactories.add(new ObjectDataValueFactory());
        metaValueFactories.add(new ByteArrayValueFactory());
        metaValueFactories.add(new DataValueMetaFactory());
        metaValueFactories.add(new MLTextValueFactory());
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

        if (LibsUtils.isJacksonPresent()) {
            metaValueFactories.add(new JacksonJsonNodeValueFactory());
        }

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

    public final synchronized DtoSchemaResolver getDtoMetaResolver() {
        if (dtoMetaResolver == null) {
            dtoMetaResolver = createDtoMetaResolver();
        }
        return dtoMetaResolver;
    }

    protected DtoSchemaResolver createDtoMetaResolver() {
        return new DtoSchemaResolver(this);
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
        return new DefaultRecordTypeService(this);
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

    protected List<AttProcessor> getAttProcessors() {
        return Arrays.asList(
            new AttFormatProcessor(),
            new AttPrefixSuffixProcessor(),
            new AttOrProcessor()
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
