package ru.citeck.ecos.records2

import mu.KotlinLogging
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.ExceptionUtils.throwException
import ru.citeck.ecos.commons.utils.LibsUtils.isJacksonPresent
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorServiceImpl
import ru.citeck.ecos.records2.evaluator.evaluators.*
import ru.citeck.ecos.records2.graphql.RecordsMetaGql
import ru.citeck.ecos.records2.graphql.meta.value.MetaValuesConverter
import ru.citeck.ecos.records2.graphql.types.GqlMetaQueryDef
import ru.citeck.ecos.records2.graphql.types.GqlTypeDefinition
import ru.citeck.ecos.records2.graphql.types.MetaEdgeTypeDef
import ru.citeck.ecos.records2.graphql.types.MetaValueTypeDef
import ru.citeck.ecos.records2.meta.AttributesMetaResolver
import ru.citeck.ecos.records2.meta.RecordsMetaService
import ru.citeck.ecos.records2.meta.RecordsMetaServiceImpl
import ru.citeck.ecos.records2.meta.RecordsTemplateService
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.PredicateServiceImpl
import ru.citeck.ecos.records2.predicate.api.records.PredicateRecords
import ru.citeck.ecos.records2.predicate.json.std.PredicateJsonDeserializer
import ru.citeck.ecos.records2.predicate.json.std.PredicateJsonSerializer
import ru.citeck.ecos.records2.predicate.json.std.PredicateTypes
import ru.citeck.ecos.records2.querylang.QueryLangService
import ru.citeck.ecos.records2.querylang.QueryLangServiceImpl
import ru.citeck.ecos.records2.request.rest.RestHandler
import ru.citeck.ecos.records2.resolver.LocalRecordsResolverV0
import ru.citeck.ecos.records2.source.dao.RecordsDao
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDao
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDaoAttsProvider
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDaoAttsProviderImpl
import ru.citeck.ecos.records2.source.dao.local.source.RecordsSourceRecordsDao
import ru.citeck.ecos.records2.type.DefaultRecTypeService
import ru.citeck.ecos.records2.type.RecordTypeService
import ru.citeck.ecos.records3.record.dao.impl.group.RecordsGroupDao
import ru.citeck.ecos.records3.record.op.atts.service.RecordAttsService
import ru.citeck.ecos.records3.record.op.atts.service.RecordAttsServiceImpl
import ru.citeck.ecos.records3.record.op.atts.service.proc.*
import ru.citeck.ecos.records3.record.op.atts.service.schema.read.AttSchemaReader
import ru.citeck.ecos.records3.record.op.atts.service.schema.read.DtoSchemaReader
import ru.citeck.ecos.records3.record.op.atts.service.schema.read.proc.AttProcReader
import ru.citeck.ecos.records3.record.op.atts.service.schema.resolver.AttSchemaResolver
import ru.citeck.ecos.records3.record.op.atts.service.schema.resolver.computed.ComputedAttsService
import ru.citeck.ecos.records3.record.op.atts.service.schema.write.AttSchemaGqlWriter
import ru.citeck.ecos.records3.record.op.atts.service.schema.write.AttSchemaWriter
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValuesConverter
import ru.citeck.ecos.records3.record.op.atts.service.value.factory.*
import ru.citeck.ecos.records3.record.op.atts.service.value.factory.bean.BeanValueFactory
import ru.citeck.ecos.records3.record.resolver.LocalRecordsResolver
import ru.citeck.ecos.records3.record.resolver.LocalRecordsResolverImpl
import ru.citeck.ecos.records3.record.resolver.LocalRemoteResolver
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver
import ru.citeck.ecos.records3.rest.RestHandlerAdapter
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.function.Consumer
import java.util.function.Supplier

open class RecordsServiceFactory {

    companion object {
        val log = KotlinLogging.logger {}
    }

    val attributesMetaResolver: AttributesMetaResolver by lazy { createAttributesMetaResolver() }
    val restHandlerAdapter: RestHandlerAdapter by lazy { createRestHandlerAdapter() }
    val recordsMetaGql: RecordsMetaGql by lazy { createRecordsMetaGql() }
    val restHandler: RestHandler by lazy { createRestHandler() }
    val recordsService: RecordsService by lazy { createRecordsService() }
    val recordsServiceV1: ru.citeck.ecos.records3.RecordsService by lazy { createRecordsServiceV1() }
    val dtoSchemaReader: DtoSchemaReader by lazy { createDtoSchemaReader() }
    val recordsResolver: LocalRemoteResolver by lazy { createRecordsResolver() }
    val predicateService: PredicateService by lazy { createPredicateService() }
    val queryLangService: QueryLangService by lazy { createQueryLangService() }
    val recordsMetaService: RecordsMetaService by lazy { createRecordsMetaService() }
    val recordsAttsService: RecordAttsService by lazy { createRecordsAttsService() }
    val remoteRecordsResolver: RemoteRecordsResolver? by lazy { createRemoteRecordsResolver() }
    val attValuesConverter: AttValuesConverter by lazy { createAttValuesConverter() }
    val recordEvaluatorService: RecordEvaluatorService by lazy { createRecordEvaluatorService() }
    val predicateJsonDeserializer: PredicateJsonDeserializer by lazy { createPredicateJsonDeserializer() }
    val predicateTypes: PredicateTypes by lazy { createPredicateTypes() }
    val recordTypeService: RecordTypeService by lazy { createRecordTypeService() }
    val recordsTemplateService: RecordsTemplateService by lazy { createRecordsTemplateService() }
    val attProcService: AttProcService by lazy { createAttProcService() }
    val attSchemaReader: AttSchemaReader by lazy { createAttSchemaReader() }
    val attSchemaWriter: AttSchemaWriter by lazy { createAttSchemaWriter() }
    val attSchemaResolver: AttSchemaResolver by lazy { createAttSchemaResolver() }
    val metaValuesConverter: MetaValuesConverter by lazy { createMetaValuesConverter() }
    val attProcReader: AttProcReader by lazy { createAttProcReader() }
    val computedAttsService: ComputedAttsService by lazy { createComputedAttsService() }

    @Deprecated("")
    val queryContextSupplier: Supplier<out QueryContext> by lazy { createQueryContextSupplier() }

    val attValueFactories: List<AttValueFactory<*>> by  lazy { createAttValueFactories() }

    @Deprecated("")
    val localRecordsResolverV0: LocalRecordsResolverV0 by lazy { createLocalRecordsResolverV0() }
    val localRecordsResolver: LocalRecordsResolver by lazy { createLocalRecordsResolver() }

    val metaRecordsDaoAttsProvider: MetaRecordsDaoAttsProvider by lazy { createMetaRecordsDaoAttsProvider() }

    val properties: RecordsProperties by lazy { createProperties() }

    private val defaultRecordsDao: List<*> by lazy { createDefaultRecordsDao() }

    private var tmpEvaluatorsService: RecordEvaluatorService? = null
    private var tmpRecordsService: ru.citeck.ecos.records3.RecordsService? = null
    private var tmpRecordsServiceV0: RecordsService? = null

    val gqlTypes: List<GqlTypeDefinition> by lazy { createGqlTypes() }

    private var isJobsInitialized = false

    init {
        Json.context.addDeserializer(predicateJsonDeserializer)
        Json.context.addSerializer(PredicateJsonSerializer())
    }

    protected open fun createMetaValuesConverter(): MetaValuesConverter {
        return MetaValuesConverter(this)
    }

    protected open fun createAttributesMetaResolver(): AttributesMetaResolver {
        return AttributesMetaResolver()
    }

    protected open fun createRecordsMetaGql(): RecordsMetaGql {
        return RecordsMetaGql(this)
    }

    protected open fun createGqlTypes(): List<GqlTypeDefinition> {
        val gqlTypes: MutableList<GqlTypeDefinition> = ArrayList()
        val metaValueTypeDef = MetaValueTypeDef(this)
        gqlTypes.add(metaValueTypeDef)
        gqlTypes.add(GqlMetaQueryDef(this))
        gqlTypes.add(MetaEdgeTypeDef(metaValueTypeDef))
        return gqlTypes
    }

    @Synchronized
    open fun initJobs(executor: ScheduledExecutorService?) {
        if (!isJobsInitialized) {
            log.info("Records jobs initialization started. Executor: $executor")
            localRecordsResolver.initJobs(executor)
            isJobsInitialized = true
        }
    }

    protected open fun createPredicateTypes(): PredicateTypes {
        return PredicateTypes()
    }

    @Synchronized
    protected open fun createPredicateJsonDeserializer(): PredicateJsonDeserializer {
        return PredicateJsonDeserializer(predicateTypes)
    }

    protected open fun createRecordEvaluatorService(): RecordEvaluatorService {
        if (tmpEvaluatorsService != null) {
            return tmpEvaluatorsService!!
        }
        val service: RecordEvaluatorService = RecordEvaluatorServiceImpl(this)
        tmpEvaluatorsService = service
        service.register(GroupEvaluator())
        service.register(PredicateEvaluator())
        service.register(AlwaysTrueEvaluator())
        service.register(AlwaysFalseEvaluator())
        service.register(HasAttributeEvaluator())
        service.register(HasPermissionEvaluator())
        tmpEvaluatorsService = null
        return service
    }

    protected open fun createRecordsService(): RecordsService {
        if (tmpRecordsServiceV0 != null) {
            return tmpRecordsServiceV0!!
        }
        tmpRecordsServiceV0 = RecordsServiceImpl(this)
        for (dao in defaultRecordsDao) {
            if (dao is RecordsDao) {
                tmpRecordsServiceV0!!.register(dao)
            }
        }
        val result: RecordsService = tmpRecordsServiceV0!!
        tmpRecordsServiceV0 = null
        return result
    }

    protected open fun createRecordsServiceV1(): ru.citeck.ecos.records3.RecordsService {
        if (tmpRecordsService != null) {
            return tmpRecordsService!!
        }
        val recordsService = ru.citeck.ecos.records3.RecordsServiceImpl(this)
        tmpRecordsService = recordsService
        for (dao in defaultRecordsDao) {
            if (dao is ru.citeck.ecos.records3.record.dao.RecordsDao) {
                recordsService.register(dao)
            }
        }
        tmpRecordsService = null
        return recordsService
    }

    protected open fun createDefaultRecordsDao() : List<*> {
        return listOf(
            MetaRecordsDao(this),
            RecordsSourceRecordsDao(),
            PredicateRecords(),
            RecordsGroupDao(),
            ru.citeck.ecos.records2.source.common.group.RecordsGroupDao()
        )
    }

    protected open fun createRecordsResolver(): LocalRemoteResolver {
        return LocalRemoteResolver(this)
    }

    protected open fun createRemoteRecordsResolver(): RemoteRecordsResolver? {
        return null
    }

    protected open fun createLocalRecordsResolver(): LocalRecordsResolver {
        return LocalRecordsResolverImpl(this)
    }

    protected open fun createLocalRecordsResolverV0(): LocalRecordsResolverV0 {
        return LocalRecordsResolverV0(this)
    }

    protected open fun createQueryLangService(): QueryLangService {
        return QueryLangServiceImpl()
    }

    protected open fun createRecordsMetaService(): RecordsMetaService {
        return RecordsMetaServiceImpl(this)
    }

    protected open fun createRecordsAttsService(): RecordAttsService {
        return RecordAttsServiceImpl(this)
    }

    protected open fun createPredicateService(): PredicateService {
        return PredicateServiceImpl()
    }

    protected open fun createAttValuesConverter(): AttValuesConverter {
        return AttValuesConverter(this)
    }

    protected open fun createAttValueFactories(): List<AttValueFactory<*>> {
        val metaValueFactories: MutableList<AttValueFactory<*>> = ArrayList()
        metaValueFactories.add(ObjectDataValueFactory())
        metaValueFactories.add(ByteArrayValueFactory())
        metaValueFactories.add(DataValueAttFactory())
        metaValueFactories.add(MLTextValueFactory())
        metaValueFactories.add(RecordAttValueFactory())
        metaValueFactories.add(BeanValueFactory())
        metaValueFactories.add(BooleanValueFactory())
        metaValueFactories.add(DateValueFactory())
        metaValueFactories.add(DoubleValueFactory())
        metaValueFactories.add(IntegerValueFactory())
        metaValueFactories.add(JsonNodeValueFactory())
        metaValueFactories.add(LongValueFactory())
        metaValueFactories.add(StringValueFactory())
        metaValueFactories.add(RecordRefValueFactory(this))
        if (isJacksonPresent()) {
            metaValueFactories.add(JacksonJsonNodeValueFactory())
        }
        return metaValueFactories
    }

    protected open fun createDtoSchemaReader(): DtoSchemaReader {
        return DtoSchemaReader(this)
    }

    protected open fun createQueryContextSupplier(): Supplier<out QueryContext> {
        return Supplier { QueryContext() }
    }

    @Synchronized
    fun createQueryContext(): QueryContext {
        val context = queryContextSupplier.get()
        context.serviceFactory = this
        return context
    }

    protected open fun createRestHandler(): RestHandler {
        return RestHandler(this)
    }

    protected open fun createRestHandlerAdapter(): RestHandlerAdapter {
        return RestHandlerAdapter(this)
    }

    protected open fun createProperties(): RecordsProperties {
        return RecordsProperties()
    }

    protected open fun createMetaRecordsDaoAttsProvider(): MetaRecordsDaoAttsProvider {
        return MetaRecordsDaoAttsProviderImpl()
    }

    protected open fun createRecordTypeService(): RecordTypeService {
        return DefaultRecTypeService(this)
    }

    protected open fun createRecordsTemplateService(): RecordsTemplateService {
        return RecordsTemplateService(this)
    }

    protected open fun createAttSchemaReader(): AttSchemaReader {
        return AttSchemaReader(this)
    }

    protected open fun createAttSchemaWriter(): AttSchemaWriter {
        return AttSchemaGqlWriter()
    }

    protected open fun createAttSchemaResolver(): AttSchemaResolver {
        return AttSchemaResolver(this)
    }

    protected open fun getAttProcessors(): List<AttProcessor> {
        return listOf(
            AttFormatProcessor(),
            AttPrefixSuffixProcessor(),
            AttOrElseProcessor(),
            AttJoinProcessor(),
            AttCastProcessor()
        )
    }

    protected open fun createAttProcService(): AttProcService {
        val service = AttProcService(this)
        getAttProcessors().forEach(Consumer { processor -> service.register(processor) })
        return service
    }

    protected open fun createAttProcReader(): AttProcReader {
        return AttProcReader()
    }

    protected open fun createComputedAttsService(): ComputedAttsService {
        return ComputedAttsService()
    }
}
