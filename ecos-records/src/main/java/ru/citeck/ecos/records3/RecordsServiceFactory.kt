package ru.citeck.ecos.records3

import mu.KotlinLogging
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.LibsUtils.isJacksonPresent
import ru.citeck.ecos.records2.QueryContext
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorServiceImpl
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
import ru.citeck.ecos.records2.source.dao.local.job.JobExecutor
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDao
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDaoAttsProvider
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDaoAttsProviderImpl
import ru.citeck.ecos.records3.cache.CacheManager
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
import ru.citeck.ecos.records3.record.dao.impl.api.RecordsApiRecordsDao
import ru.citeck.ecos.records3.record.dao.impl.group.RecordsGroupDao
import ru.citeck.ecos.records3.record.dao.impl.source.RecordsSourceRecordsDao
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.ctxatts.CtxAttsProvider
import ru.citeck.ecos.records3.record.request.ctxatts.CtxAttsService
import ru.citeck.ecos.records3.record.request.ctxatts.StdCtxAttsProvider
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
import ru.citeck.ecos.webapp.api.context.EcosWebAppContext
import ru.citeck.ecos.webapp.api.properties.EcosWebAppProperties
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import java.util.function.Supplier

@Suppress("LeakingThis")
open class RecordsServiceFactory {

    companion object {
        val log = KotlinLogging.logger {}
    }

    val restHandlerAdapter: RestHandlerAdapter by lazySingleton { createRestHandlerAdapter() }
    val restHandler: RestHandler by lazySingleton { createRestHandler() }
    val recordsService: ru.citeck.ecos.records2.RecordsService by lazySingleton { createRecordsService() }
    val recordsServiceV1: RecordsService by lazySingleton { createRecordsServiceV1() }
    val dtoSchemaReader: DtoSchemaReader by lazySingleton { createDtoSchemaReader() }
    val recordsResolver: LocalRemoteResolver by lazySingleton { createRecordsResolver() }
    val predicateService: PredicateService by lazySingleton { createPredicateService() }
    val queryLangService: QueryLangService by lazySingleton { createQueryLangService() }
    val recordsAttsService: RecordAttsService by lazySingleton { createRecordsAttsService() }
    val remoteRecordsResolver: RemoteRecordsResolver? by lazySingleton { createRemoteRecordsResolver() }
    val attValuesConverter: AttValuesConverter by lazySingleton { createAttValuesConverter() }
    val recordEvaluatorService: RecordEvaluatorService by lazySingleton { createRecordEvaluatorService() }
    val predicateJsonDeserializer: PredicateJsonDeserializer by lazySingleton { createPredicateJsonDeserializer() }
    val predicateTypes: PredicateTypes by lazySingleton { createPredicateTypes() }
    val recordsTemplateService: RecordsTemplateService by lazySingleton { createRecordsTemplateService() }
    val recordTypeService: RecordTypeService by lazySingleton { createRecordTypeService() }
    val attProcService: AttProcService by lazySingleton { createAttProcService() }
    val attSchemaReader: AttSchemaReader by lazySingleton { createAttSchemaReader() }
    val attSchemaWriter: AttSchemaWriter by lazySingleton { createAttSchemaWriter() }
    val attSchemaResolver: AttSchemaResolver by lazySingleton { createAttSchemaResolver() }
    val metaValuesConverter: MetaValuesConverter by lazySingleton { createMetaValuesConverter() }
    val attProcReader: AttProcReader by lazySingleton { createAttProcReader() }
    val recordComputedAttsService: RecordComputedAttsService by lazySingleton { createRecordComputedAttsService() }
    val recordsTxnService: RecordsTxnService by lazySingleton { createRecordsTxnService() }
    val ctxAttsProviders: List<CtxAttsProvider> by lazySingleton { createCtxAttsProviders() }
    val ctxAttsService: CtxAttsService by lazySingleton { CtxAttsService(this) }
    val jobExecutor: JobExecutor by lazySingleton { createJobExecutor() }
    val txnActionManager: TxnActionManager by lazySingleton { createTxnActionManager() }

    internal val cacheManager: CacheManager by lazySingleton { CacheManager(this) }

    @Deprecated("")
    val queryContextSupplier: Supplier<out QueryContext> by lazySingleton { createQueryContextSupplier() }

    val attValueFactories: List<AttValueFactory<*>> by lazySingleton { createAttValueFactories() }

    @Deprecated("")
    val localRecordsResolverV0: LocalRecordsResolverV0 by lazySingleton { createLocalRecordsResolverV0() }
    val localRecordsResolver: LocalRecordsResolver by lazySingleton { createLocalRecordsResolver() }

    val metaRecordsDaoAttsProvider: MetaRecordsDaoAttsProvider by lazySingleton { createMetaRecordsDaoAttsProvider() }

    val properties: RecordsProperties by lazySingleton {
        val props = createProperties()
        val webappProps = this.webappProps
        if (!webappProps.gatewayMode && props.defaultApp.isNotEmpty()) {
            log.warn { "DefaultApp can't be used without gatewayMode. DefaultApp: ${props.defaultApp}" }
            props.withDefaultApp("")
        } else {
            props
        }
    }

    val webappProps by lazySingleton {
        getEcosWebAppContext()?.getProperties() ?: EcosWebAppProperties("", "")
    }

    val defaultRecordsDao: List<*> by lazySingleton { createDefaultRecordsDao() }

    private var tmpEvaluatorsService: RecordEvaluatorService? = null
    private var tmpRecordsService: RecordsService? = null
    private var tmpRecordsServiceV0: ru.citeck.ecos.records2.RecordsService? = null

    private var recordTypeServiceImpl: RecordTypeService? = null

    init {
        Json.context.addDeserializer(predicateJsonDeserializer)
        Json.context.addSerializer(PredicateJsonSerializer())

        RequestContext.setLastCreatedServices(this)
    }

    protected open fun createJobExecutor(): JobExecutor {
        return JobExecutor(this)
    }

    protected open fun createCtxAttsProviders(): List<CtxAttsProvider> {
        val providers = ArrayList<CtxAttsProvider>()
        providers.add(StdCtxAttsProvider(this))
        return providers
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

    protected open fun createPredicateTypes(): PredicateTypes {
        return PredicateTypes()
    }

    @Synchronized
    protected open fun createPredicateJsonDeserializer(): PredicateJsonDeserializer {
        return PredicateJsonDeserializer(predicateTypes)
    }

    protected open fun createRecordEvaluatorService(): RecordEvaluatorService {
        return RecordEvaluatorServiceImpl()
    }

    protected open fun createRecordsService(): ru.citeck.ecos.records2.RecordsService {
        return ru.citeck.ecos.records2.RecordsServiceImpl(this)
    }

    protected open fun createRecordsServiceV1(): RecordsService {
        return RecordsServiceImpl(this)
    }

    protected open fun createDefaultRecordsDao(): List<*> {
        return listOf(
            MetaRecordsDao(this),
            RecordsSourceRecordsDao(this),
            PredicateRecords(),
            RecordsGroupDao(),
            RecordsApiRecordsDao(),
            ru.citeck.ecos.records2.source.common.group.RecordsGroupDao()
        )
    }

    protected open fun createRecordsResolver(): LocalRemoteResolver {
        return LocalRemoteResolver(this)
    }

    protected open fun createRemoteRecordsResolver(): RemoteRecordsResolver? {
        val ctx = this.getEcosWebAppContext()
        val webClient = ctx?.getWebClient()
        return if (webClient != null) {
            RemoteRecordsResolver(this)
        } else {
            check(!webappProps.gatewayMode) {
                "WebAppContext should not be null in gateway mode! Props: $properties"
            }
            log.warn("EcosWebAppContext does not exists. Remote records requests wont be allowed")
            null
        }
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

        val doubleValueFactory = DoubleValueFactory()
        val floatValueFactory = FloatValueFactory()
        val booleanValueFactory = BooleanValueFactory()
        val stringValueFactory = StringValueFactory()
        val integerValueFactory = IntegerValueFactory()
        val longValueFactory = LongValueFactory()

        metaValueFactories.add(doubleValueFactory)
        metaValueFactories.add(floatValueFactory)
        metaValueFactories.add(booleanValueFactory)
        metaValueFactories.add(stringValueFactory)
        metaValueFactories.add(integerValueFactory)
        metaValueFactories.add(longValueFactory)

        val dataValueFactory = DataValueAttFactory()
        val instantValueFactory = InstantValueFactory()

        metaValueFactories.add(ObjectDataValueFactory())
        metaValueFactories.add(ByteArrayValueFactory())
        metaValueFactories.add(dataValueFactory)
        metaValueFactories.add(MLTextValueFactory())
        metaValueFactories.add(RecordAttsValueFactory())
        metaValueFactories.add(BeanValueFactory())
        metaValueFactories.add(DateValueFactory())
        metaValueFactories.add(instantValueFactory)
        metaValueFactories.add(OffsetDateTimeValueFactory())
        metaValueFactories.add(JsonNodeValueFactory())
        metaValueFactories.add(RecordRefValueFactory())
        metaValueFactories.add(EntityWithMetaValueFactory())
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
        return RecordsProperties.DEFAULT
    }

    protected open fun createMetaRecordsDaoAttsProvider(): MetaRecordsDaoAttsProvider {
        return MetaRecordsDaoAttsProviderImpl()
    }

    protected open fun createRecordsTemplateService(): RecordsTemplateService {
        return RecordsTemplateService()
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
            AttHexProcessor(),
            AttYamlProcessor(),
            AttPlusProcessor()
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

    open fun getEcosWebAppContext(): EcosWebAppContext? {
        return null
    }

    fun setRecordTypeService(recordTypeService: RecordTypeService) {
        this.recordTypeServiceImpl = recordTypeService
    }

    private fun <T> lazySingleton(initializer: () -> T): Lazy<T> {
        val initializationInProgress = AtomicBoolean()
        var createdValue: T? = null
        return lazy {
            if (initializationInProgress.compareAndSet(false, true)) {
                val value = initializer()
                createdValue = value
                if (value is ServiceFactoryAware) {
                    value.setRecordsServiceFactory(this)
                }
                value
            } else {
                createdValue ?: error("Cyclic reference")
            }
        }
    }
}
