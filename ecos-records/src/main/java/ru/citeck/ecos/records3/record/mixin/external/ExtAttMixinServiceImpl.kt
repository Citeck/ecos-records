package ru.citeck.ecos.records3.record.mixin.external

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records2.source.dao.local.job.PeriodicJob
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.read.AttSchemaReader
import ru.citeck.ecos.records3.record.atts.schema.read.DtoSchemaReader
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttSchemaResolver
import ru.citeck.ecos.records3.record.atts.schema.write.AttSchemaWriter
import ru.citeck.ecos.records3.record.type.RecordTypeService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.promise.Promise
import ru.citeck.ecos.webapp.api.promise.Promises
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet

class ExtAttMixinServiceImpl : ExtAttMixinService, ServiceFactoryAware {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val configurers = CopyOnWriteArrayList<ExtAttMixinConfigurer>()
    private var externalAttsByType: Map<String, Map<String, ExtAttData>> = emptyMap()
    private var nonLocalAttsByType: Map<String, Map<String, ExtAttData>> = emptyMap()
    private val externalAttsCtxByTypeCache = ConcurrentHashMap<String, Optional<ExtAttMixinContextImpl>>()
    private val nonLocalExtAttsByTypeCache = ConcurrentHashMap<String, Optional<ExtAttMixinContextImpl>>()

    private val configurersVersion = AtomicLong()
    private val externalAttsVersion = AtomicLong()
    private val nextLazyUpdateTimeMs = AtomicLong(-1)

    private lateinit var dtoSchemaReader: DtoSchemaReader
    private lateinit var schemaReader: AttSchemaReader
    private lateinit var schemaWriter: AttSchemaWriter
    private lateinit var recordsService: RecordsService
    private lateinit var attSchemaResolver: AttSchemaResolver
    private lateinit var recordTypeService: RecordTypeService

    private val nonLocalAttsWatchers = CopyOnWriteArrayList<(Map<String, Map<String, ExtAttData>>) -> Unit>()

    private fun updateAttributesIfRequired(wasUpdated: AtomicBoolean? = null): Long {

        val configurersVersion = this.configurersVersion.get()
        val extAttsVersion = this.externalAttsVersion.get()
        if (configurersVersion == extAttsVersion) {
            return extAttsVersion
        }

        log.debug { "Reconfigure attributes for version $configurersVersion" }

        val newAttsByType = HashMap<String, MutableMap<String, ExtAttData>>()
        val newNonLocalAttsByType = HashMap<String, MutableMap<String, ExtAttData>>()
        configurers.forEach { configurer ->
            val cfg = ConfigImpl(configurer)
            configurer.configure(cfg)
            for (attData in cfg.getAttsData()) {
                val typeAtts = newAttsByType.computeIfAbsent(attData.typeId) { HashMap() }
                val existingData = typeAtts[attData.attribute]
                if (existingData != null &&
                    existingData.configurer !== configurer &&
                    existingData.priority >= attData.priority
                ) {
                    continue
                }
                typeAtts[attData.attribute] = attData
                if (!attData.local) {
                    val nonLocalAtts = newNonLocalAttsByType.computeIfAbsent(attData.typeId) { HashMap() }
                    nonLocalAtts[attData.attribute] = attData
                }
            }
        }
        synchronized(externalAttsVersion) {
            wasUpdated?.set(true)
            this.externalAttsByType = newAttsByType
            this.nonLocalAttsByType = newNonLocalAttsByType
            this.externalAttsCtxByTypeCache.clear()
            this.nonLocalExtAttsByTypeCache.clear()
            this.externalAttsVersion.set(configurersVersion)
            nonLocalAttsWatchers.forEach {
                try {
                    it.invoke(newNonLocalAttsByType)
                } catch (e: Throwable) {
                    log.error(e) { "Error while watcher invocation" }
                }
            }
            return externalAttsVersion.get()
        }
    }

    override fun setRecordsServiceFactory(serviceFactory: RecordsServiceFactory) {

        dtoSchemaReader = serviceFactory.dtoSchemaReader
        schemaReader = serviceFactory.attSchemaReader
        schemaWriter = serviceFactory.attSchemaWriter
        recordsService = serviceFactory.recordsService
        attSchemaResolver = serviceFactory.attSchemaResolver
        recordTypeService = serviceFactory.recordTypeService

        serviceFactory.jobExecutor.addSystemJob(object : PeriodicJob {
            override fun getInitDelay(): Long = TimeUnit.MINUTES.toMillis(1)
            override fun execute(): Boolean {
                val nextLazyUpdateTimeMsValue = nextLazyUpdateTimeMs.get()
                if (nextLazyUpdateTimeMsValue != -1L && System.currentTimeMillis() >= nextLazyUpdateTimeMsValue) {
                    updateAttributesIfRequired()
                    nextLazyUpdateTimeMs.set(-1L)
                }
                return false
            }

            override fun getPeriod(): Long {
                return TimeUnit.SECONDS.toMillis(10)
            }
        })
        serviceFactory.getEcosWebAppApi()?.doWhenAppReady { updateAttributesIfRequired() }
    }

    private fun reconfigure() {
        configurersVersion.incrementAndGet()
        nextLazyUpdateTimeMs.set(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10))
    }

    override fun getNonLocalAtts(): Map<String, Map<String, ExtAttData>> {
        updateAttributesIfRequired()
        return nonLocalAttsByType
    }

    override fun watchNonLocalAtts(action: (Map<String, Map<String, ExtAttData>>) -> Unit) {
        nonLocalAttsWatchers.add(action)
        val wasUpdated = AtomicBoolean()
        updateAttributesIfRequired(wasUpdated)
        if (!wasUpdated.get()) {
            action.invoke(nonLocalAttsByType)
        }
    }

    override fun getNonLocalExtMixinContext(typeId: String): ExtAttMixinContext? {
        return getExtMixinContext(typeId, true)
    }

    override fun getExtMixinContext(typeId: String): ExtAttMixinContext? {
        return getExtMixinContext(typeId, false)
    }

    private fun getExtMixinContext(typeId: String, nonLocal: Boolean): ExtAttMixinContext? {

        val extAttsVersion = updateAttributesIfRequired()

        val (
            cache,
            attsByType
        ) = if (nonLocal) {
            nonLocalExtAttsByTypeCache to nonLocalAttsByType
        } else {
            externalAttsCtxByTypeCache to externalAttsByType
        }

        if (attsByType.isEmpty()) {
            return null
        }
        val ctx = cache[typeId]
        if (ctx != null) {
            return ctx.orElse(null)
        }
        val attributes = LinkedHashMap<String, ExtAttData>()
        fillTypeAtts(typeId, attsByType, attributes)
        val newCtx: ExtAttMixinContextImpl? = if (attributes.isEmpty()) {
            null
        } else {
            ExtAttMixinContextImpl(attributes)
        }
        synchronized(externalAttsVersion) {
            if (extAttsVersion == externalAttsVersion.get()) {
                cache[typeId] = Optional.ofNullable(newCtx)
            }
        }
        return newCtx
    }

    @Synchronized
    private fun fillTypeAtts(
        typeId: String,
        attsByType: Map<String, Map<String, ExtAttData>>,
        result: MutableMap<String, ExtAttData>,
        processedTypes: MutableSet<String> = HashSet()
    ) {
        if (typeId.isEmpty()) {
            return
        }
        if (!processedTypes.add(typeId)) {
            return
        }
        attsByType[typeId]?.forEach { (k, v) ->
            result.putIfAbsent(k, v)
        }
        val recType = recordTypeService.getRecordType(
            EntityRef.create(AppName.EMODEL, "type", typeId)
        )
        fillTypeAtts(recType.getParentId(), attsByType, result, processedTypes)
    }

    override fun register(mixin: ExtAttMixinConfigurer) {
        configurers.add(mixin)
        configurersVersion.incrementAndGet()
    }

    private class ExtAttMixinContextImpl(private val attributes: Map<String, ExtAttData>) : ExtAttMixinContext {

        private var providedAtts: Set<String> = attributes.keys

        override fun getProvidedAtts(): Set<String> {
            return providedAtts
        }

        override fun getRequiredAttsFor(attributes: Set<String>): Set<String> {
            val result = LinkedHashSet<String>()
            for (att in attributes) {
                val attData = this.attributes[att]
                if (attData != null) {
                    result.addAll(attData.requiredAtts.values)
                }
            }
            return result
        }

        override fun getAtt(
            handlerContext: ExtAttHandlerContext,
            requiredAtts: Map<String, Any?>,
            attToLoad: SchemaAtt
        ): Promise<Any?> {

            val attData = attributes[attToLoad.name] ?: return Promises.resolve(null)

            val attsForHandler = LinkedHashMap<String, Any?>()
            attData.requiredAtts.forEach { (k, v) ->
                attsForHandler[k] = requiredAtts[v]
            }
            val handlerRes = attData.handler.invoke(handlerContext, attsForHandler, attToLoad)
            return if (handlerRes is Promise<Any?>) {
                handlerRes
            } else {
                Promises.resolve(handlerRes)
            }
        }
    }

    private inner class ConfigImpl(val configurer: ExtAttMixinConfigurer) : ExtMixinConfig {

        var providedAtts: MutableList<ProvidedAttConfigImpl> = ArrayList()
        var commonProvidedAtt = ProvidedAttConfigImpl(emptyList()).setLocal(false)
        var commonHandler: (ExtAttHandlerContext, String, Map<String, Any?>, SchemaAtt) -> Any? = { _, _, _, _ -> null }

        override fun setEcosType(typeId: String): ExtMixinConfig {
            commonProvidedAtt.setEcosType(typeId)
            return this
        }

        override fun setLocal(local: Boolean): ExtMixinConfig {
            commonProvidedAtt.setLocal(local)
            return this
        }

        override fun addProvidedAtt(attribute: String): ExtMixinConfig.ProvidedAttConfig {
            return addProvidedAtts(listOf(attribute))
        }

        override fun addProvidedAtts(vararg attributes: String): ExtMixinConfig.ProvidedAttsConfig {
            return addProvidedAtts(attributes.toList())
        }

        override fun addProvidedAtts(attributes: Collection<String>): ProvidedAttConfigImpl {
            val newConfig = ProvidedAttConfigImpl(attributes.toList())
            providedAtts.add(newConfig)
            return newConfig
        }

        override fun addRequiredAtts(requiredAtts: Map<String, *>): ExtMixinConfig {
            commonProvidedAtt.addRequiredAtts(requiredAtts)
            return this
        }

        override fun addRequiredAtts(requiredAtts: Class<*>): ExtMixinConfig {
            commonProvidedAtt.addRequiredAtts(requiredAtts)
            return this
        }

        override fun setPriority(priority: Float): ExtMixinConfig {
            commonProvidedAtt.setPriority(priority)
            return this
        }

        override fun <T : Any> withHandler(type: Class<T>, handler: (String, T) -> Any?): ExtMixinConfig {
            return withHandler { name, data ->
                handler.invoke(
                    name,
                    dtoSchemaReader.instantiateNotNull(type, data)
                )
            }
        }

        override fun withHandler(handler: (String, ObjectData) -> Any?): ExtMixinConfig {
            withRawHandler { _, name, data, _ -> handler.invoke(name, ObjectData.create(data)) }
            return this
        }

        override fun withRawHandler(handler: (ExtAttHandlerContext, String, Map<String, Any?>, SchemaAtt) -> Any?): ExtMixinConfig {
            this.commonHandler = handler
            return this
        }

        override fun reconfigure() {
            this@ExtAttMixinServiceImpl.reconfigure()
        }

        fun getAttsData(): List<ExtAttData> {
            val result = ArrayList<ExtAttData>()
            for (attsCfg in providedAtts) {
                if (attsCfg.providedAttributes.isEmpty()) {
                    continue
                }
                val priority = attsCfg.priority ?: commonProvidedAtt.priority ?: 0f
                val typeId = attsCfg.typeId.ifBlank {
                    commonProvidedAtt.typeId.ifBlank {
                        error("You should define ECOS type for provided attributes: ${attsCfg.providedAttributes}")
                    }
                }
                for (attName in attsCfg.providedAttributes) {
                    val requiredAtts = LinkedHashMap(commonProvidedAtt.requireAtts)
                    requiredAtts.putAll(attsCfg.requireAtts)
                    result.add(
                        ExtAttData(
                            typeId,
                            attsCfg.local ?: commonProvidedAtt.local ?: false,
                            priority,
                            attName,
                            requiredAtts,
                            configurer,
                            attsCfg.handler ?: { ctx, data, att -> commonHandler.invoke(ctx, attName, data, att) }
                        )
                    )
                }
            }
            return result
        }
    }

    private inner class ProvidedAttConfigImpl(
        val providedAttributes: List<String>
    ) : ExtMixinConfig.ProvidedAttConfig, ExtMixinConfig.ProvidedAttsConfig {

        val requireAtts = LinkedHashMap<String, String>()
        var priority: Float? = null
        var local: Boolean? = null
        var handler: ((ExtAttHandlerContext, Map<String, Any?>, SchemaAtt) -> Any?)? = null
        var typeId: String = ""

        override fun setEcosType(typeId: String): ProvidedAttConfigImpl {
            this.typeId = typeId
            return this
        }

        override fun setLocal(local: Boolean): ProvidedAttConfigImpl {
            this.local = local
            return this
        }

        override fun addRequiredAtts(requiredAtts: Map<String, *>): ProvidedAttConfigImpl {
            addRequiredAtts(schemaReader.read(requiredAtts))
            return this
        }

        override fun addRequiredAtts(requiredAtts: Class<*>): ProvidedAttConfigImpl {
            addRequiredAtts(dtoSchemaReader.read(requiredAtts))
            return this
        }

        private fun addRequiredAtts(atts: List<SchemaAtt>) {
            requireAtts.putAll(schemaWriter.writeToMap(atts))
        }

        override fun setPriority(priority: Float): ProvidedAttConfigImpl {
            this.priority = priority
            return this
        }

        override fun withHandler(handler: (ObjectData) -> Any?): ExtMixinConfig.ProvidedAttConfig {
            withRawHandler { _, data, _ -> handler.invoke(ObjectData.create(data)) }
            return this
        }

        override fun withRawHandler(handler: (ExtAttHandlerContext, Map<String, Any?>, SchemaAtt) -> Any?): ProvidedAttConfigImpl {
            this.handler = handler
            return this
        }

        override fun <T : Any> withHandler(type: Class<T>, handler: (T) -> Any?): ExtMixinConfig.ProvidedAttConfig {
            return withHandler { data -> handler.invoke(dtoSchemaReader.instantiateNotNull(type, data)) }
        }

        override fun withHandler(handler: (String, ObjectData) -> Any?): ProvidedAttConfigImpl {
            return withRawHandler { _, data, att -> handler.invoke(att.name, ObjectData.create(data)) }
        }

        override fun <T : Any> withHandler(
            type: Class<T>,
            handler: (String, T) -> Any?
        ): ProvidedAttConfigImpl {
            return withRawHandler { _, data, att ->
                handler.invoke(att.name, dtoSchemaReader.instantiateNotNull(type, ObjectData.create(data)))
            }
        }
    }
}
