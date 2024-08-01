package ru.citeck.ecos.records3.record.atts.schema.resolver

import ecos.com.fasterxml.jackson210.databind.node.NullNode
import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.json.exception.JsonMapperException
import ru.citeck.ecos.commons.promise.Promises
import ru.citeck.ecos.commons.utils.LibsUtils
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue
import ru.citeck.ecos.records2.request.error.ErrorUtils
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAtt
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAttValue
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAttsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.proc.AttProcDef
import ru.citeck.ecos.records3.record.atts.proc.AttProcService
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.read.AttSchemaReader
import ru.citeck.ecos.records3.record.atts.schema.read.DtoSchemaReader
import ru.citeck.ecos.records3.record.atts.schema.utils.AttStrUtils
import ru.citeck.ecos.records3.record.atts.schema.write.AttSchemaWriter
import ru.citeck.ecos.records3.record.atts.utils.RecTypeUtils
import ru.citeck.ecos.records3.record.atts.value.*
import ru.citeck.ecos.records3.record.atts.value.factory.EntityRefValueFactory
import ru.citeck.ecos.records3.record.atts.value.impl.AttEdgeValue
import ru.citeck.ecos.records3.record.atts.value.impl.AttFuncValue
import ru.citeck.ecos.records3.record.atts.value.impl.EmptyAttValue
import ru.citeck.ecos.records3.record.atts.value.impl.NullAttValue
import ru.citeck.ecos.records3.record.mixin.MixinContext
import ru.citeck.ecos.records3.record.mixin.external.ExtAttHandlerContext
import ru.citeck.ecos.records3.record.mixin.external.ExtAttMixinContext
import ru.citeck.ecos.records3.record.mixin.external.ExtAttMixinService
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import ru.citeck.ecos.records3.record.type.RecordTypeService
import ru.citeck.ecos.records3.utils.AttUtils
import ru.citeck.ecos.records3.utils.RecordRefUtils
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.promise.Promise
import java.time.Duration
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashMap
import kotlin.streams.toList
import com.fasterxml.jackson.databind.node.NullNode as JackNullNode

class AttSchemaResolver : ServiceFactoryAware {

    companion object {
        val log = KotlinLogging.logger {}

        const val CTX_SOURCE_ID_KEY: String = "ctx-source-id"

        private val ID_SCALARS = setOf(
            ScalarType.LOCAL_ID,
            ScalarType.APP_NAME,
            ScalarType.ID,
            ScalarType.ASSOC
        )
    }

    private lateinit var services: RecordsServiceFactory
    private lateinit var attValuesConverter: AttValuesConverter
    private lateinit var attProcService: AttProcService
    private lateinit var attSchemaReader: AttSchemaReader
    private lateinit var attSchemaWriter: AttSchemaWriter
    private lateinit var dtoSchemaReader: DtoSchemaReader
    private lateinit var recordsService: RecordsService
    private lateinit var computedAttsService: RecordComputedAttsService
    private lateinit var extAttMixinService: ExtAttMixinService

    private lateinit var recordTypeService: RecordTypeService

    private lateinit var currentAppName: String

    override fun setRecordsServiceFactory(serviceFactory: RecordsServiceFactory) {
        this.services = serviceFactory
        this.attValuesConverter = serviceFactory.attValuesConverter
        this.attProcService = serviceFactory.attProcService
        this.attSchemaReader = serviceFactory.attSchemaReader
        this.attSchemaWriter = serviceFactory.attSchemaWriter
        this.dtoSchemaReader = serviceFactory.dtoSchemaReader
        this.recordsService = serviceFactory.recordsServiceV1
        this.computedAttsService = serviceFactory.recordComputedAttsService
        this.recordTypeService = serviceFactory.recordTypeService
        this.extAttMixinService = serviceFactory.extAttMixinService
        this.currentAppName = serviceFactory.getEcosWebAppApi()?.getProperties()?.appName ?: ""
    }

    fun resolve(args: ResolveArgs): List<RecordAtts> {
        return resolveRaw(args).map {
            RecordAtts(it.first, ObjectData.create(it.second))
        }
    }

    fun resolveRaw(args: ResolveArgs): List<Pair<EntityRef, Map<String, Any?>>> {
        val context = AttContext.getCurrent()
        return if (context == null) {
            AttContext.doWithCtx(services) { resolveInAttCtx(args) }
        } else {
            resolveInAttCtx(args)
        }
    }

    fun getFlatAttributes(atts: List<SchemaAtt>, expandProcAtts: Boolean): List<SchemaAtt> {
        var schemaAttsToLoad: List<SchemaAtt> = atts
        if (expandProcAtts) {
            schemaAttsToLoad = expandAttsWithProcAtts(schemaAttsToLoad)
        }
        return AttSchemaUtils.simplifySchema(schemaAttsToLoad)
    }

    private fun convertToAttValue(value: Any?, initReferencable: Boolean = true): AttValue {
        val result = if (value is AttValue) {
            value
        } else {
            attValuesConverter.toAttValue(value)
        } ?: NullAttValue.INSTANCE
        if (result !is EntityRefValueFactory.EntityRefValue || initReferencable) {
            // At this moment we will wait until async initialization will be completed
            // in this method, but in future this waiting may be moved outside for various optimizations
            result.init()?.get()
        }
        return result
    }

    private fun convertToAttValues(values: List<Any?>): List<AttValue> {

        if (values.isEmpty()) {
            return emptyList()
        }
        if (values.size == 1) {
            return listOf(convertToAttValue(values[0]))
        }
        // Threshold for 5 elements required to avoid creation unnecessary
        // object instances when all values are not referencable.
        val isAttsPreloadingRequired = values.size > 5 || values.any {
            it is EntityRef || it is EntityRefValueFactory.EntityRefValue
        }
        if (!isAttsPreloadingRequired) {
            return values.map { convertToAttValue(it) }
        }
        val refs = ArrayList<EntityRef>()
        val refsIdx = ArrayList<Int>()
        val attValues = values.mapIndexed { idx, value ->
            val res = convertToAttValue(value, false)
            if (res is EntityRefValueFactory.EntityRefValue) {
                refs.add(res.getRef())
                refsIdx.add(idx)
            }
            res
        }
        if (refs.isEmpty()) {
            return attValues
        }
        val attsToLoad = EntityRefValueFactory.getAttsToLoad(
            AttContext.getCurrentSchemaAtt().inner,
            attSchemaWriter
        )
        if (attsToLoad.isNotEmpty()) {
            val resolvedAtts = recordsService.getAtts(refs, attsToLoad, true)
            for (idx in refsIdx) {
                (attValues[idx] as? EntityRefValueFactory.EntityRefValue)?.init(resolvedAtts[idx].getAtts())
            }
        } else {
            val emptyAtts = ObjectData.create()
            for (idx in refsIdx) {
                (attValues[idx] as? EntityRefValueFactory.EntityRefValue)?.init(emptyAtts)
            }
        }
        return attValues
    }

    private fun resolveInAttCtx(args: ResolveArgs): List<Pair<EntityRef, Map<String, Any?>>> {

        val context = ResolveContext(args.mixinCtx, recordTypeService)

        val values: List<AttValue> = convertToAttValues(args.values)
        val attValuesContext = ArrayList<ValueContext>()

        for (i in values.indices) {
            val defaultRef = if (args.defaultValueRefs.isEmpty()) {
                EntityRef.EMPTY
            } else {
                args.defaultValueRefs[i]
            }
            attValuesContext.add(context.toRootValueContext(this, values[i], defaultRef))
        }

        var expandedAtts = args.attributes
        if (!args.rawAtts) {
            expandedAtts = expandAttsWithProcAtts(expandedAtts)
        }

        val flattenAtts = getFlatAttributes(expandedAtts, false)
        val result = resolveRoot(attValuesContext, flattenAtts, context)
        val resultAttsMap = resolveResultsWithAliases(result, expandedAtts, args.rawAtts)

        return attValuesContext.mapIndexed { idx, value ->
            value.getRef() to resultAttsMap[idx]
        }
    }

    private fun expandAttsWithProcAtts(atts: List<SchemaAtt>): List<SchemaAtt> {
        if (atts.isEmpty()) {
            return atts
        }
        val expandedAtts = atts.mapTo(ArrayList()) {
            it.withInner(expandAttsWithProcAtts(it.inner))
        }
        expandedAtts.addAll(attProcService.getProcessorsAtts(atts))
        return expandedAtts
    }

    private fun resolveResultsWithAliases(
        values: List<Map<String, Any?>>,
        atts: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<Map<String, Any?>> {
        if (atts.isEmpty()) {
            return values.map { emptyMap() }
        }
        val currentAtt: SchemaAtt = SchemaAtt.create()
            .withName("root")
            .withInner(atts)
            .build()

        val result = ArrayList<Map<String, Any?>>()
        for (value in values) {
            result.add(resolveResultWithAliases(currentAtt, value, rawAtts))
        }
        return result
    }

    private fun resolveResultWithAliases(
        currentAtt: SchemaAtt,
        value: Map<String, Any?>,
        rawAtts: Boolean
    ): Map<String, Any?> {

        val resolved = resolveWithAliases(currentAtt, value, false, rawAtts, true)
        if (resolved is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            return resolved as Map<String, Any?>
        }
        throw IllegalStateException(
            "Expected Map, but found " + resolved +
                ". Value: " + value + " atts: " + currentAtt
        )
    }

    private fun resolveWithAliases(
        currentAtt: SchemaAtt,
        valueArg: Any?,
        isMultiple: Boolean,
        rawAtts: Boolean,
        root: Boolean
    ): Any? {
        var value = valueArg
        val atts: List<SchemaAtt> = currentAtt.inner
        if (value == null || atts.isEmpty()) {
            return value
        }
        if (isMultiple) {
            return (value as List<*>).map {
                resolveWithAliases(currentAtt, it, false, rawAtts, root)
            }
        } else {
            if (value is List<*>) {
                val valueList = value
                if (valueList.isEmpty()) {
                    return null
                }
                value = valueList[0]
            }
        }
        return if (value is Map<*, *>) {
            if (!root && !rawAtts && atts.size == 1 && atts[0].processors.isEmpty()) {
                val att = atts[0]
                val attValue = value[att.name]
                return resolveWithAliases(att, attValue, att.multiple, rawAtts = false, root = false)
            }
            val processors = HashMap<String, List<AttProcDef>>()
            val result = LinkedHashMap<String, Any?>()
            for (att in atts) {
                val attValue = value[att.name]
                result[att.getAliasForValue()] = resolveWithAliases(
                    att,
                    attValue,
                    att.multiple,
                    rawAtts,
                    false
                )
                if (att.processors.isNotEmpty()) {
                    processors[att.getAliasForValue()] = att.processors
                }
            }
            val processedAtts = attProcService.applyProcessors(result, processors)
            if (!root && !rawAtts && atts.size == 1) {
                processedAtts[atts[0].getAliasForValue()]
            } else {
                processedAtts
            }
        } else {
            throw IllegalStateException("Unknown value: $value. Atts: $atts")
        }
    }

    private fun processExtMixins(values: List<ValueContext>, resolveCtx: ResolveContext, attsToLoad: List<SchemaAtt>) {

        val attsToLoadByName = attsToLoad.associateBy { it.name }
        val extAttMixinContexts = HashMap<String, ExtMixinCtxData?>()
        for (attValueCtx in values) {
            if (attValueCtx.value is EntityRefValueFactory.EntityRefValue) {
                // mixin attributes for references will be evaluated in external request
                continue
            }
            val typeId = attValueCtx.getTypeRef().getLocalId()
            if (typeId.isBlank()) {
                continue
            }
            if (extAttMixinContexts.containsKey(typeId)) {
                val existingData = extAttMixinContexts[typeId]
                existingData?.values?.add(attValueCtx)
            } else {
                val ctx = extAttMixinService.getExtMixinContext(typeId)
                val attsToLoadFromMixinNames = HashSet<String>()
                val attsToLoadFromMixin = ctx?.getProvidedAtts()?.mapNotNull {
                    val attToLoad = attsToLoadByName[it]
                    if (attToLoad != null) {
                        attsToLoadFromMixinNames.add(it)
                    }
                    attToLoad
                }

                val ctxData = if (attsToLoadFromMixin.isNullOrEmpty()) {
                    null
                } else {
                    ExtMixinCtxData(
                        ctx,
                        attsToLoadFromMixin,
                        attSchemaReader.read(ctx.getRequiredAttsFor(attsToLoadFromMixinNames))
                    )
                }
                extAttMixinContexts[typeId] = ctxData
                ctxData?.values?.add(attValueCtx)
            }
        }

        if (extAttMixinContexts.isEmpty()) {
            return
        }

        val handlerContext = ExtAttHandlerContextImpl()

        val getExtAttsPromises = ArrayList<Promise<List<Map<String, Any?>>>>()

        for (extMixinCtxData in extAttMixinContexts.values) {
            extMixinCtxData ?: continue

            val attsToEvalExtMixinAtts = HashMap<HashCodeWrapper<Map<String, Any?>>, MutableList<ValueContext>>()
            val reqAttsValues = resolveAtts(resolveCtx, extMixinCtxData.values, "", extMixinCtxData.requiredAtts)

            for ((idx, value) in reqAttsValues.withIndex()) {
                attsToEvalExtMixinAtts.computeIfAbsent(HashCodeWrapper(value)) { ArrayList() }
                    .add(extMixinCtxData.values[idx])
            }

            val reqAttsList = attsToEvalExtMixinAtts.keys.toList()
            val promise = Promises.all(
                reqAttsList.map { reqAtts ->
                    Promises.all(
                        extMixinCtxData.attsToLoad.map { schemaAtt ->
                            extMixinCtxData.context.getAtt(
                                handlerContext,
                                reqAtts.obj,
                                schemaAtt
                            ).then {
                                schemaAtt.name to it
                            }
                        }
                    ).then {
                        it.toMap()
                    }
                }
            ).then { extAttsList ->
                for ((idx, extAtts) in extAttsList.withIndex()) {
                    attsToEvalExtMixinAtts[reqAttsList[idx]]?.forEach { it.setPrecomputedAtts(extAtts) }
                }
                extAttsList
            }
            getExtAttsPromises.add(promise)
        }
        Promises.all(getExtAttsPromises).get(Duration.ofMinutes(5))
    }

    private fun resolveAtts(
        resolveCtx: ResolveContext,
        valuesCtx: List<ValueContext>,
        basePath: String,
        schemaAtts: List<SchemaAtt>
    ): List<Map<String, Any?>> {

        if (valuesCtx.isEmpty()) {
            return emptyList()
        }
        if (schemaAtts.isEmpty()) {
            return valuesCtx.map { emptyMap() }
        }
        val attPathBefore: String = resolveCtx.path
        resolveCtx.path = basePath
        return try {
            val currentAtt: SchemaAtt = SchemaAtt.create()
                .withName("root")
                .withInner(schemaAtts)
                .build()
            val simpleAtts = AttSchemaUtils.simplifySchema(schemaAtts)
            valuesCtx.map {
                val result = resolve(it, simpleAtts, resolveCtx)
                resolveResultWithAliases(currentAtt, result, false)
            }
        } finally {
            resolveCtx.path = attPathBefore
        }
    }

    private fun resolveRoot(
        values: List<ValueContext>,
        attributes: List<SchemaAtt>,
        context: ResolveContext
    ): List<Map<String, Any?>> {

        processExtMixins(values, context, attributes)

        return values.map { resolveRoot(it, attributes, context) }
    }

    private fun resolveRoot(
        value: ValueContext,
        attributes: List<SchemaAtt>,
        context: ResolveContext
    ): Map<String, Any?> {

        val rootBefore: ValueContext = context.rootValue
        context.rootValue = value
        return try {
            resolve(value, attributes, context)
        } finally {
            context.rootValue = rootBefore
        }
    }

    private fun resolve(
        values: List<ValueContext>,
        attributes: List<SchemaAtt>,
        context: ResolveContext
    ): List<Map<String, Any?>> {
        return values.stream()
            .map { ctx -> resolve(ctx, attributes, context) }
            .collect(Collectors.toList())
    }

    private fun resolve(
        value: ValueContext,
        attributes: List<SchemaAtt>,
        context: ResolveContext
    ): Map<String, Any?> {

        val result: MutableMap<String, Any?> = LinkedHashMap()
        val attContext: AttContext = context.attContext
        val currentSchemaAtt = attContext.getSchemaAtt()
        val currentValuePath: String = context.path

        val disabledMixinPaths: MutableSet<String> = context.disabledMixinPaths
        val disabledComputedPaths: MutableSet<String> = context.disabledComputedPaths

        for (att in attributes) {

            val attPath = if (currentValuePath.isEmpty()) {
                att.name
            } else {
                currentValuePath + (if (!att.isScalar()) "." else "") + att.name
            }

            context.path = attPath
            attContext.setSchemaAtt(att)
            attContext.setAttPath(attPath)

            var attValue: Any?
            var attName = att.name

            if (currentValuePath.isEmpty() && attName.startsWith("$")) {

                val contextAttName = attName.substring(1)
                attValue = context.reqContext.ctxData.ctxAtts[contextAttName]
            } else {

                if (attName.length > 2 && attName[0] == '\\' && attName[1] == '$') {
                    attName = attName.substring(1)
                }

                val computedAtts: Map<String, RecordComputedAtt> = value.computedAtts
                val computedAtt: RecordComputedAtt? = computedAtts[attName]

                if (computedAtt != null && disabledComputedPaths.add(attPath)) {
                    attValue = try {
                        val valueCtx = getContextForDynamicAtt(value, computedAtt.id)
                        if (valueCtx != null) {
                            withoutSourceIdMapping(context) {
                                computedAttsService.compute(
                                    AttValueResolveCtx(
                                        currentValuePath,
                                        context,
                                        valueCtx
                                    ),
                                    computedAtt
                                )
                            }
                        } else {
                            log.debug { "Value context is not found for attribute $computedAtt" }
                        }
                    } catch (e: Exception) {
                        val msg = "Resolving error. Path: $attPath. Att: $computedAtt"
                        context.reqContext.addMsg(MsgLevel.ERROR) { msg }
                        log.error(msg, e)
                        null
                    } finally {
                        disabledComputedPaths.remove(attPath)
                    }
                } else {

                    val mixinAttCtx = context.mixinCtx.getMixin(attPath)

                    attValue = if (mixinAttCtx != null && disabledMixinPaths.add(attPath)) {
                        try {
                            val mixinValueCtx = getContextForDynamicAtt(value, mixinAttCtx.path)
                            if (mixinValueCtx == null) {
                                null
                            } else {
                                withoutSourceIdMapping(context) {
                                    mixinAttCtx.mixin.getAtt(
                                        mixinAttCtx.path,
                                        AttValueResolveCtx(
                                            currentValuePath,
                                            context,
                                            mixinValueCtx
                                        )
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            val msg = "Resolving error. Path: $attPath"
                            context.reqContext.addMsg(MsgLevel.ERROR) { msg }
                            log.error(msg, e)
                            null
                        } finally {
                            disabledMixinPaths.remove(attPath)
                        }
                    } else {
                        value.resolve(attContext)
                    }
                }
            }

            if (attValue is RecordComputedAttValue) {
                val notNullAttValue = attValue
                attValue = withoutSourceIdMapping(context) {
                    computedAttsService.compute(
                        AttValueResolveCtx(
                            currentValuePath,
                            context,
                            value
                        ),
                        notNullAttValue
                    )
                }
            } else if (attValue is Function0<*>) {
                attValue = attValue.invoke()
            }

            val attValues = AttValueUtils.rawToListWithoutNullValues(attValue)
            val alias = att.getAliasForValue()
            if (att.multiple) {
                var valuesStream = attValues.stream()
                if (att.inner.size == 1) {
                    val scalar = ScalarType.getBySchema(att.inner[0].name)
                    if (scalar != null && ID_SCALARS.contains(scalar)) {
                        valuesStream = valuesStream.filter { !AttValueUtils.isEmpty(it) }
                    }
                }
                val contextValues = convertToAttValues(valuesStream.toList())
                    .map { context.toValueContext(this, value, it) }
                result[alias] = resolve(contextValues, att.inner, context)
            } else {
                if (attValues.isEmpty()) {
                    result[alias] = null
                } else {
                    if (att.isScalar()) {
                        result[alias] = attValues[0]
                    } else {
                        val attValueForCtx = convertToAttValue(attValues[0])
                        val valueContext = context.toValueContext(this, value, attValueForCtx)
                        result[alias] = resolve(valueContext, att.inner, context)
                    }
                }
            }
        }
        context.path = currentValuePath
        attContext.setSchemaAtt(currentSchemaAtt)
        attContext.setAttPath(currentValuePath)

        return result
    }

    private inline fun <T> withoutSourceIdMapping(context: ResolveContext, crossinline action: () -> T): T {
        val sourceIdMapping = context.reqContext.ctxData.sourceIdMapping
        return if (sourceIdMapping.isNotEmpty()) {
            RequestContext.doWithCtx({ it.withoutSourceIdMapping() }) {
                action.invoke()
            }
        } else {
            action.invoke()
        }
    }

    private fun getContextForDynamicAtt(value: ValueContext?, path: String): ValueContext? {
        if (AttStrUtils.indexOf(path, ".") == -1) {
            return value
        }
        var valueCtx = value
        val valuePathList = AttStrUtils.split(path, ".")
        for (i in 1 until valuePathList.size) {
            if (valueCtx == null) {
                break
            }
            valueCtx = valueCtx.parent
        }
        return if (valueCtx != null && path.contains("?")) {
            valueCtx.parent
        } else {
            valueCtx
        }
    }

    private class ValueContext(
        val resolver: AttSchemaResolver?,
        val resolveCtx: ResolveContext?,
        val parent: ValueContext?,
        val value: AttValue,
        private val defaultValueRef: EntityRef,
        val ctxSourceId: String,
        val context: RequestContext?,
        val computedAtts: Map<String, RecordComputedAtt>,
        val isRootValue: Boolean
    ) {

        companion object {
            val EMPTY = ValueContext(
                null,
                null,
                null,
                EmptyAttValue.INSTANCE,
                EntityRef.EMPTY,
                "",
                null,
                emptyMap(),
                isRootValue = false
            )
        }

        private var precomputedAtts: Map<String, Any?> = emptyMap()

        private val typeRefValue: EntityRef by lazy {
            RecTypeUtils.anyTypeToRef(value.type)
        }

        private val computedRawRef: EntityRef by lazy {
            var result = run {
                val id = value.id
                var computedRef: EntityRef = if (
                    id == null ||
                    id is String && StringUtils.isBlank(id) ||
                    id is EntityRef && id.isEmpty()
                ) {
                    if (defaultValueRef.isNotEmpty()) {
                        defaultValueRef
                    } else if (isRootValue) {
                        EntityRef.create(ctxSourceId, UUID.randomUUID().toString())
                    } else {
                        EntityRef.EMPTY
                    }
                } else if (id is EntityRef) {
                    id
                } else {
                    var mutId = id
                    if (mutId is DataValue) {
                        mutId = mutId.asText()
                    }
                    if (mutId is String) {
                        if (mutId.contains(EntityRef.SOURCE_ID_DELIMITER)) {
                            EntityRef.valueOf(mutId)
                        } else if (defaultValueRef.isNotEmpty()) {
                            EntityRef.create(defaultValueRef.getAppName(), defaultValueRef.getSourceId(), mutId)
                        } else {
                            EntityRef.create(ctxSourceId, mutId)
                        }
                    } else {
                        EntityRef.create(ctxSourceId, id.toString())
                    }
                }
                if (ctxSourceId.isNotEmpty() &&
                    computedRef.getAppName().isEmpty() &&
                    computedRef.getSourceId().isEmpty() &&
                    computedRef.getLocalId().isNotEmpty()
                ) {
                    computedRef = EntityRef.create(ctxSourceId, computedRef.getLocalId())
                }
                computedRef
            }
            if (EntityRef.isEmpty(result)) {
                result
            } else {
                val currentAppName = resolver?.currentAppName ?: ""
                if (result.getAppName().isBlank() && currentAppName.isNotBlank()) {
                    result = result.withDefaultAppName(currentAppName)
                }
                result
            }
        }

        private val computedRef: EntityRef by lazy {
            val currentAppName = resolver?.currentAppName ?: ""
            RecordRefUtils.mapAppIdAndSourceId(
                computedRawRef,
                currentAppName,
                context?.ctxData?.sourceIdMapping
            )
        }

        /**
         * Get value id to identify it when error occurs. Should not be used in business logic.
         */
        private fun getValueIdentifierStrSafe(): String {
            return if (EntityRef.isNotEmpty(defaultValueRef)) {
                defaultValueRef
            } else {
                try {
                    value.id
                } catch (e: Throwable) {
                    log.error(e) { "value.getId throws exception" }
                    "ERROR"
                }
            }.toString()
        }

        fun setPrecomputedAtts(precomputedAtts: Map<String, Any?>?) {
            this.precomputedAtts = precomputedAtts ?: emptyMap()
        }

        fun resolve(attContext: AttContext): Any? {

            val schemaAtt = attContext.getSchemaAtt()
            val name = schemaAtt.name

            if (log.isTraceEnabled) {
                log.trace("Resolve $schemaAtt")
            }

            val res: Any? = try {
                resolveImpl(name)
            } catch (e: Throwable) {
                log.error {
                    "Attribute resolving error. " +
                        "Value ID: '${getValueIdentifierStrSafe()}' " +
                        "Path: '${resolveCtx?.path}' " +
                        "Attribute: '$name' " +
                        "Value type: '${value::class.qualifiedName}' " +
                        "Message: '${e.message}' " +
                        "RequestId: '${context?.ctxData?.requestId}'"
                }
                if (context == null || !context.ctxData.omitErrors) {
                    throw e
                }
                context.addMsg(MsgLevel.ERROR) {
                    ErrorUtils.convertException(e, context.getServices())
                }
                null
            }

            if (log.isTraceEnabled) {
                log.trace("Result: $res")
            }
            return res
        }

        private fun resolveImpl(attribute: String): Any? {
            if (RecordConstants.ATT_NULL == attribute) {
                return null
            }
            val isLocalIdOrAppNameSchema = attribute == ScalarType.LOCAL_ID.schema ||
                attribute == ScalarType.APP_NAME.schema
            // special case for ?localId and ?appName because it is not equal to any other scalars
            if (value is AttValueProxy && (isLocalIdOrAppNameSchema || !attribute.startsWith('?'))) {
                val res = value.getAtt(attribute)
                return if (isLocalIdOrAppNameSchema) {
                    when (res) {
                        is String -> res
                        is AttValue -> res.asText()
                        is MetaValue -> res.string
                        else -> res.toString()
                    }
                } else {
                    res
                }
            }
            val scalarType = ScalarType.getBySchemaOrMirrorAtt(attribute)
            return if (scalarType != null) {
                getScalar(scalarType, attribute)
            } else {
                when (attribute) {
                    RecordConstants.ATT_TYPE,
                    RecordConstants.ATT_ECOS_TYPE -> typeRefValue

                    RecordConstants.ATT_AS -> AttFuncValue { type -> value.getAs(type) }
                    RecordConstants.ATT_HAS -> AttFuncValue { name -> value.has(name) }
                    RecordConstants.ATT_EDGE -> AttFuncValue { name -> AttEdgeValue(value.getEdge(name)) }
                    RecordConstants.ATT_SELF -> value
                    RecordConstants.ATT_ID -> value.getAtt(RecordConstants.ATT_ID) ?: getLocalId()
                    else -> {
                        if (precomputedAtts.containsKey(attribute)) {
                            precomputedAtts[attribute]
                        } else {
                            value.getAtt(
                                if (attribute.startsWith("\\_")) {
                                    attribute.substring(1)
                                } else {
                                    attribute
                                }
                            )
                        }
                    }
                }
            }
        }

        fun getRef() = computedRef

        fun getRawRef() = computedRawRef

        fun getAppName() = getRef().getAppName().ifBlank { resolver?.currentAppName ?: "" }

        fun getLocalId() = getRawRef().getLocalId()

        fun getTypeRef() = typeRefValue

        private fun getScalar(scalar: ScalarType, attribute: String): Any? {
            return when (scalar) {
                ScalarType.STR -> value.asText()
                ScalarType.DISP -> {
                    val disp = value.displayName

                    if (disp == null || LibsUtils.isJacksonPresent() && disp is JackNullNode) {
                        null
                    } else if (disp is DataValue) {
                        when {
                            disp.isNull() -> null
                            disp.isObject() -> {
                                return try {
                                    val mlText = disp.getAsNotNull(MLText::class.java)
                                    mlText.getClosestValue(I18nContext.getLocale())
                                } catch (e: JsonMapperException) {
                                    null
                                }
                            }

                            else -> disp.asText()
                        }
                    } else {
                        when (disp) {
                            is NullNode -> null
                            is String -> disp
                            is MLText -> {
                                if (attribute.startsWith('?')) {
                                    MLText.getClosestValue(disp, I18nContext.getLocale())
                                } else {
                                    disp
                                }
                            }

                            else -> disp.toString()
                        }
                    }
                }

                ScalarType.ID,
                ScalarType.ASSOC -> getRef().toString()

                ScalarType.LOCAL_ID -> getLocalId()
                ScalarType.APP_NAME -> getAppName()
                ScalarType.NUM -> value.asDouble()
                ScalarType.BOOL -> value.asBoolean()
                ScalarType.JSON -> {
                    var json = value.asJson()
                    json = if (json is String) {
                        Json.mapper.read(json)
                    } else {
                        Json.mapper.toJson(json)
                    }
                    json
                }

                ScalarType.RAW -> {
                    when (val raw = value.asRaw()) {
                        is DataValue -> raw
                        is String -> DataValue.createStr(raw)
                        else -> DataValue.create(raw)
                    }
                }

                ScalarType.BIN -> {
                    when (val bin = value.asBin()) {
                        is ByteArray -> bin
                        is String -> strScalarToBinaryValue(bin)
                        is DataValue -> {
                            if (bin.isBinary()) {
                                bin.binaryValue()
                            } else if (bin.isTextual()) {
                                strScalarToBinaryValue(bin.textValue())
                            } else {
                                Json.mapper.toBytes(value)
                            }
                        }

                        else -> Json.mapper.toBytes(bin)
                    }
                }
            }
        }

        private fun strScalarToBinaryValue(text: String): ByteArray {
            return try {
                Base64.getDecoder().decode(text)
            } catch (e: IllegalArgumentException) {
                text.toByteArray()
            }
        }
    }

    private class ResolveContext(
        val mixinCtx: MixinContext,
        val recordTypeService: RecordTypeService
    ) {
        val attContext: AttContext = AttContext.getCurrentNotNull()
        val reqContext: RequestContext = RequestContext.getCurrentNotNull()
        val disabledMixinPaths: MutableSet<String> = HashSet()
        val disabledComputedPaths: MutableSet<String> = HashSet()
        var path: String = ""
        var rootValue: ValueContext = ValueContext.EMPTY

        fun toRootValueContext(
            resolver: AttSchemaResolver,
            attValue: AttValue,
            defaultValueRef: EntityRef
        ): ValueContext {
            if (attValue is NullAttValue) {
                return ValueContext.EMPTY
            }
            return ValueContext(
                resolver,
                this,
                null,
                attValue,
                defaultValueRef,
                reqContext.getVar(CTX_SOURCE_ID_KEY)
                    ?: "",
                reqContext,
                getComputedAtts(null, attValue),
                isRootValue = true
            )
        }

        fun toValueContext(resolver: AttSchemaResolver, parent: ValueContext?, attValue: AttValue): ValueContext {
            if (attValue is NullAttValue) {
                return ValueContext.EMPTY
            }
            return ValueContext(
                resolver,
                this,
                parent,
                attValue,
                EntityRef.EMPTY,
                reqContext.getVar(CTX_SOURCE_ID_KEY)
                    ?: "",
                reqContext,
                getComputedAtts(parent, attValue),
                isRootValue = false
            )
        }

        private fun getComputedAtts(parent: ValueContext?, value: AttValue?): Map<String, RecordComputedAtt> {
            if (value is AttValueProxy) {
                return emptyMap()
            }
            val computedAtts = HashMap<String, RecordComputedAtt>()
            parent?.computedAtts?.forEach { (id, att) ->
                val dotIdx: Int = AttStrUtils.indexOf(id, ".")
                if (dotIdx > 0 && dotIdx < id.length + 1) {
                    computedAtts[id.substring(dotIdx + 1)] = att
                }
            }
            try {
                val typeRef = RecTypeUtils.anyTypeToRef(value?.type)
                if (EntityRef.isNotEmpty(typeRef)) {
                    for (att in recordTypeService.getRecordType(typeRef).getComputedAtts()) {
                        if (AttUtils.isValidComputedAtt(att.id, false)) {
                            val scalar = ScalarType.getBySchemaOrMirrorAtt(att.id)
                            if (scalar != null) {
                                if (scalar.overridable) {
                                    computedAtts.putIfAbsent(scalar.schema, att)
                                    computedAtts.putIfAbsent(scalar.mirrorAtt, att)
                                }
                            } else {
                                computedAtts[att.id] = att
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                log.error("Computed atts resolving error", e)
            }
            return computedAtts
        }
    }

    private inner class AttValueResolveCtx(
        val basePath: String,
        val resolveCtx: ResolveContext,
        val valueCtx: ValueContext
    ) : AttValueCtx {

        override fun getTypeRef(): EntityRef {
            return valueCtx.getTypeRef()
        }

        override fun getTypeId(): String {
            return valueCtx.getTypeRef().getLocalId()
        }

        override fun getValue(): Any {
            return valueCtx.value
        }

        override fun getRef(): EntityRef {
            return EntityRef.valueOf(valueCtx.getRef())
        }

        override fun getRawRef(): EntityRef {
            return valueCtx.getRawRef()
        }

        override fun getLocalId(): String {
            return valueCtx.getLocalId()
        }

        override fun getAtt(attribute: String): DataValue {
            return getAtts(Collections.singletonMap("k", attribute))["k"]
        }

        override fun <T : Any> getAtts(attributes: Class<T>): T {
            val schema = dtoSchemaReader.read(attributes)
            return dtoSchemaReader.instantiate(attributes, getAttsBySchema(schema))
                ?: error("Attributes class can't be instantiated. Class: $attributes Schema: $schema")
        }

        override fun getAtts(attributes: Collection<String>): ObjectData {
            return getAtts(AttUtils.toMap(attributes))
        }

        override fun getAtts(attributes: Map<String, *>): ObjectData {
            return getAttsBySchema(attSchemaReader.read(attributes))
        }

        private fun getAttsBySchema(schemaAtts: List<SchemaAtt>): ObjectData {
            if (schemaAtts.isEmpty()) {
                return ObjectData.create()
            }
            val attPathBefore: String = resolveCtx.path
            resolveCtx.path = basePath
            return try {
                val currentAtt: SchemaAtt = SchemaAtt.create()
                    .withName("root")
                    .withInner(schemaAtts)
                    .build()
                val simpleAtts = AttSchemaUtils.simplifySchema(schemaAtts)
                val result = resolve(valueCtx, simpleAtts, resolveCtx)
                ObjectData.create(resolveResultWithAliases(currentAtt, result, false))
            } finally {
                resolveCtx.path = attPathBefore
            }
        }
    }

    private class ExtMixinCtxData(
        val context: ExtAttMixinContext,
        val attsToLoad: List<SchemaAtt>,
        val requiredAtts: List<SchemaAtt>
    ) {
        val values = ArrayList<ValueContext>()
    }

    private class ExtAttHandlerContextImpl : ExtAttHandlerContext {
        private val ctxData = HashMap<Any, Any?>()
        override fun <T> computeIfAbsent(key: Any, action: (Any) -> T): T {
            @Suppress("UNCHECKED_CAST")
            return ctxData.computeIfAbsent(key, action) as T
        }
    }

    private class HashCodeWrapper<T>(val obj: T) {

        private var hashCodeEvaluated: Boolean = false
        private var hashCodeValue: Int = 0

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (javaClass != other?.javaClass) {
                return false
            }
            other as HashCodeWrapper<*>
            if (hashCode() != other.hashCode()) {
                return false
            }
            return obj == other.obj
        }

        override fun hashCode(): Int {
            if (hashCodeEvaluated) {
                return hashCodeValue
            }
            hashCodeValue = obj.hashCode()
            hashCodeEvaluated = true
            return hashCodeValue
        }
    }
}
