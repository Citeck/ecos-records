package ru.citeck.ecos.records3

import mu.KotlinLogging
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.LibsUtils.isJacksonPresent
import ru.citeck.ecos.records2.QueryContext
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorServiceImpl
import ru.citeck.ecos.records2.evaluator.evaluators.*
import ru.citeck.ecos.records2.graphql.meta.value.MetaValuesConverter
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
import ru.citeck.ecos.records2.source.dao.local.job.JobExecutor
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDao
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDaoAttsProvider
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDaoAttsProviderImpl
import ru.citeck.ecos.records3.record.atts.RecordAttsService
import ru.citeck.ecos.records3.record.atts.RecordAttsServiceImpl
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAtt
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAttsService
import ru.citeck.ecos.records3.record.atts.proc.*
import ru.citeck.ecos.records3.record.atts.schema.read.AttSchemaReader
import ru.citeck.ecos.records3.record.atts.schema.read.DtoSchemaReader
import ru.citeck.ecos.records3.record.atts.schema.read.proc.AttProcReader
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttSchemaResolver
import ru.citeck.ecos.records3.record.atts.schema.write.AttSchemaWriter
import ru.citeck.ecos.records3.record.atts.schema.write.AttSchemaWriterV2
import ru.citeck.ecos.records3.record.atts.value.AttValuesConverter
import ru.citeck.ecos.records3.record.atts.value.factory.*
import ru.citeck.ecos.records3.record.atts.value.factory.bean.BeanValueFactory
import ru.citeck.ecos.records3.record.atts.value.factory.time.DateValueFactory
import ru.citeck.ecos.records3.record.atts.value.factory.time.InstantValueFactory
import ru.citeck.ecos.records3.record.atts.value.factory.time.OffsetDateTimeValueFactory
import ru.citeck.ecos.records3.record.dao.impl.group.RecordsGroupDao
import ru.citeck.ecos.records3.record.dao.impl.source.RecordsSourceRecordsDao
import ru.citeck.ecos.records3.record.request.ContextAttsProvider
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.resolver.LocalRecordsResolver
import ru.citeck.ecos.records3.record.resolver.LocalRecordsResolverImpl
import ru.citeck.ecos.records3.record.resolver.LocalRemoteResolver
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver
import ru.citeck.ecos.records3.record.type.RecordTypeService
import ru.citeck.ecos.records3.rest.RestHandlerAdapter
import ru.citeck.ecos.records3.txn.DefaultRecordsTxnService
import ru.citeck.ecos.records3.txn.RecordsTxnService
import ru.citeck.ecos.records3.txn.ext.TxnActionManager
import ru.citeck.ecos.records3.txn.ext.TxnActionManagerImpl
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.function.Consumer
import java.util.function.Supplier

@Suppress("LeakingThis")
open class RecordsServiceFactory {

    companion object {
        val log = KotlinLogging.logger {}
    }

    val restHandlerAdapter: RestHandlerAdapter by lazy { createRestHandlerAdapter() }
    val restHandler: RestHandler by lazy { createRestHandler() }
    val recordsService: ru.citeck.ecos.records2.RecordsService by lazy { createRecordsService() }
    val recordsServiceV1: RecordsService by lazy { createRecordsServiceV1() }
    val dtoSchemaReader: DtoSchemaReader by lazy { createDtoSchemaReader() }
    val recordsResolver: LocalRemoteResolver by lazy { createRecordsResolver() }
    val predicateService: PredicateService by lazy { createPredicateService() }
    val queryLangService: QueryLangService by lazy { createQueryLangService() }
    val recordsAttsService: RecordAttsService by lazy { createRecordsAttsService() }
    val remoteRecordsResolver: RemoteRecordsResolver? by lazy { createRemoteRecordsResolver() }
    val attValuesConverter: AttValuesConverter by lazy { createAttValuesConverter() }
    val recordEvaluatorService: RecordEvaluatorService by lazy { createRecordEvaluatorService() }
    val predicateJsonDeserializer: PredicateJsonDeserializer by lazy { createPredicateJsonDeserializer() }
    val predicateTypes: PredicateTypes by lazy { createPredicateTypes() }
    val recordsTemplateService: RecordsTemplateService by lazy { createRecordsTemplateService() }
    val recordTypeService: RecordTypeService by lazy { createRecordTypeService() }
    val attProcService: AttProcService by lazy { createAttProcService() }
    val attSchemaReader: AttSchemaReader by lazy { createAttSchemaReader() }
    val attSchemaWriter: AttSchemaWriter by lazy { createAttSchemaWriter() }
    val attSchemaResolver: AttSchemaResolver by lazy { createAttSchemaResolver() }
    val metaValuesConverter: MetaValuesConverter by lazy { createMetaValuesConverter() }
    val attProcReader: AttProcReader by lazy { createAttProcReader() }
    val recordComputedAttsService: RecordComputedAttsService by lazy { createRecordComputedAttsService() }
    val recordsTxnService: RecordsTxnService by lazy { createRecordsTxnService() }
    val defaultCtxAttsProvider: ContextAttsProvider by lazy { createDefaultCtxAttsProvider() }
    val localeSupplier: () -> Locale by lazy { createLocaleSupplier() }
    val jobExecutor: JobExecutor by lazy { createJobExecutor() }
    val txnActionManager: TxnActionManager by lazy { createTxnActionManager() }

    @Deprecated("")
    val queryContextSupplier: Supplier<out QueryContext> by lazy { createQueryContextSupplier() }

    val attValueFactories: List<AttValueFactory<*>> by lazy { createAttValueFactories() }

    @Deprecated("")
    val localRecordsResolverV0: LocalRecordsResolverV0 by lazy { createLocalRecordsResolverV0() }
    val localRecordsResolver: LocalRecordsResolver by lazy { createLocalRecordsResolver() }

    val metaRecordsDaoAttsProvider: MetaRecordsDaoAttsProvider by lazy { createMetaRecordsDaoAttsProvider() }

    val properties: RecordsProperties by lazy { createProperties() }

    private val defaultRecordsDao: List<*> by lazy { createDefaultRecordsDao() }

    private var tmpEvaluatorsService: RecordEvaluatorService? = null
    private var tmpRecordsService: RecordsService? = null
    private var tmpRecordsServiceV0: ru.citeck.ecos.records2.RecordsService? = null
    private var tmpLocalRecordsResolver: LocalRecordsResolver? = null

    private var recordTypeServiceImpl: RecordTypeService? = null

    private var isJobsInitialized = false

    init {
        Json.context.addDeserializer(predicateJsonDeserializer)
        Json.context.addSerializer(PredicateJsonSerializer())

        RequestContext.setLastCreatedServices(this)
    }

    protected open fun createJobExecutor(): JobExecutor {
        return JobExecutor(this)
    }

    protected open fun createLocaleSupplier(): () -> Locale {
        return { Locale.ENGLISH }
    }

    protected open fun createDefaultCtxAttsProvider(): ContextAttsProvider {
        return object : ContextAttsProvider {
            override fun getContextAttributes(): Map<String, Any?> = emptyMap()
        }
    }

    protected open fun createRecordTypeService(): RecordTypeService {
        return object : RecordTypeService {
            override fun getComputedAtts(typeRef: RecordRef): List<RecordComputedAtt> {
                return recordTypeServiceImpl?.getComputedAtts(typeRef) ?: emptyList()
            }
        }
    }

    protected open fun createMetaValuesConverter(): MetaValuesConverter {
        return MetaValuesConverter(this)
    }

    @Synchronized
    open fun initJobs(executor: ScheduledExecutorService?) {
        if (!isJobsInitialized) {
            log.info("Records jobs initialization started. Executor: $executor")
            jobExecutor.init(executor)
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

    protected open fun createRecordsService(): ru.citeck.ecos.records2.RecordsService {
        if (tmpRecordsServiceV0 != null) {
            return tmpRecordsServiceV0!!
        }
        tmpRecordsServiceV0 = ru.citeck.ecos.records2.RecordsServiceImpl(this)
        for (dao in defaultRecordsDao) {
            if (dao is RecordsDao) {
                tmpRecordsServiceV0!!.register(dao)
            }
        }
        val result: ru.citeck.ecos.records2.RecordsService = tmpRecordsServiceV0!!
        tmpRecordsServiceV0 = null
        return result
    }

    protected open fun createRecordsServiceV1(): RecordsService {
        if (tmpRecordsService != null) {
            return tmpRecordsService!!
        }
        val recordsService = RecordsServiceImpl(this)
        tmpRecordsService = recordsService
        for (dao in defaultRecordsDao) {
            if (dao is ru.citeck.ecos.records3.record.dao.RecordsDao) {
                recordsService.register(dao)
            }
        }
        tmpRecordsService = null
        return recordsService
    }

    protected open fun createDefaultRecordsDao(): List<*> {
        return listOf(
            MetaRecordsDao(this),
            RecordsSourceRecordsDao(this),
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
        val tmp = tmpLocalRecordsResolver
        if (tmp != null) {
            return tmp
        }
        val newResolver = LocalRecordsResolverImpl(this)
        tmpLocalRecordsResolver = newResolver
        newResolver.setRecordsTemplateService(recordsTemplateService)
        tmpLocalRecordsResolver = null
        return newResolver
    }

    protected open fun createLocalRecordsResolverV0(): LocalRecordsResolverV0 {
        return LocalRecordsResolverV0(this)
    }

    protected open fun createQueryLangService(): QueryLangService {
        return QueryLangServiceImpl()
    }

    protected open fun createRecordsAttsService(): RecordAttsService {
        return RecordAttsServiceImpl(this)
    }

    protected open fun createPredicateService(): PredicateService {
        return PredicateServiceImpl(this)
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
        metaValueFactories.add(RecordAttsValueFactory())
        metaValueFactories.add(BeanValueFactory())
        metaValueFactories.add(BooleanValueFactory())
        metaValueFactories.add(DateValueFactory())
        metaValueFactories.add(InstantValueFactory())
        metaValueFactories.add(OffsetDateTimeValueFactory())
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

    protected open fun createRecordsTemplateService(): RecordsTemplateService {
        return RecordsTemplateService(this)
    }

    protected open fun createAttSchemaReader(): AttSchemaReader {
        return AttSchemaReader(this)
    }

    protected open fun createAttSchemaWriter(): AttSchemaWriter {
        return AttSchemaWriterV2()
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
            AttCastProcessor(),
            AttRegexpGroupProcessor(),
            AttHexProcessor()
        )
    }

    protected open fun createTxnActionManager(): TxnActionManager {
        return TxnActionManagerImpl()
    }

    protected open fun createAttProcService(): AttProcService {
        val service = AttProcService(this)
        getAttProcessors().forEach(Consumer { processor -> service.register(processor) })
        return service
    }

    protected open fun createAttProcReader(): AttProcReader {
        return AttProcReader()
    }

    protected open fun createRecordComputedAttsService(): RecordComputedAttsService {
        return RecordComputedAttsService(this)
    }

    protected open fun createRecordsTxnService(): RecordsTxnService {
        return DefaultRecordsTxnService()
    }

    fun setRecordTypeService(recordTypeService: RecordTypeService) {
        this.recordTypeServiceImpl = recordTypeService
    }
}
