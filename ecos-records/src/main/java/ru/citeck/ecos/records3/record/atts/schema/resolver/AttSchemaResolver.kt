package ru.citeck.ecos.records3.record.atts.schema.resolver

import ecos.com.fasterxml.jackson210.databind.node.NullNode
import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.LibsUtils
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue
import ru.citeck.ecos.records2.request.error.ErrorUtils
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAtt
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAttValue
import ru.citeck.ecos.records3.record.atts.proc.AttProcDef
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.utils.AttStrUtils
import ru.citeck.ecos.records3.record.atts.utils.RecTypeUtils
import ru.citeck.ecos.records3.record.atts.value.*
import ru.citeck.ecos.records3.record.atts.value.impl.AttEdgeValue
import ru.citeck.ecos.records3.record.atts.value.impl.AttFuncValue
import ru.citeck.ecos.records3.record.atts.value.impl.EmptyAttValue
import ru.citeck.ecos.records3.record.atts.value.impl.NullAttValue
import ru.citeck.ecos.records3.record.mixin.MixinContext
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import ru.citeck.ecos.records3.record.type.RecordTypeService
import ru.citeck.ecos.records3.utils.AttUtils
import ru.citeck.ecos.records3.utils.RecordRefUtils
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap
import com.fasterxml.jackson.databind.node.NullNode as JackNullNode

class AttSchemaResolver(private val services: RecordsServiceFactory) {

    companion object {
        val log = KotlinLogging.logger {}

        const val CTX_SOURCE_ID_KEY: String = "ctx-source-id"

        private val ID_SCALARS = setOf(ScalarType.LOCAL_ID, ScalarType.ID, ScalarType.ASSOC)
        private val ID_SCALARS_SCHEMA = ID_SCALARS.map { it.schema }.toSet()
    }

    private val attValuesConverter = services.attValuesConverter
    private val attProcService = services.attProcService
    private val attSchemaReader = services.attSchemaReader
    private val dtoSchemaReader = services.dtoSchemaReader
    private val computedAttsService = services.recordComputedAttsService

    private val recordTypeService by lazy { services.recordTypeService }

    private val currentAppName = services.getEcosWebAppApi()?.getProperties()?.appName ?: ""

    fun resolve(args: ResolveArgs): List<Map<String, Any?>> {
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

    private fun resolveInAttCtx(args: ResolveArgs): List<Map<String, Any?>> {

        val context = ResolveContext(attValuesConverter, args.mixinCtx, recordTypeService)

        val values: List<Any?> = args.values
        val attValues = ArrayList<ValueContext>()
        for (i in values.indices) {
            val ref = if (args.valueRefs.isEmpty()) {
                RecordRef.EMPTY
            } else {
                args.valueRefs[i]
            }
            attValues.add(
                context.toRootValueContext(
                    this,
                    values[i] ?: NullAttValue.INSTANCE,
                    ref
                )
            )
        }

        var expandedAtts = args.attributes
        if (!args.rawAtts) {
            expandedAtts = expandAttsWithProcAtts(expandedAtts)
        }

        val flattenAtts = getFlatAttributes(expandedAtts, false)
        val result = resolveRoot(attValues, flattenAtts, context)
        return resolveResultsWithAliases(result, expandedAtts, args.rawAtts)
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

    private fun resolveRoot(
        values: List<ValueContext>,
        attributes: List<SchemaAtt>,
        context: ResolveContext
    ): List<Map<String, Any?>> {

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
                val values: List<ValueContext> = valuesStream
                    .map { v: Any? -> context.toValueContext(this, value, v) }
                    .collect(Collectors.toList())
                result[alias] = resolve(values, att.inner, context)
            } else {
                if (attValues.isEmpty()) {
                    result[alias] = null
                } else {
                    if (att.isScalar()) {
                        result[alias] = attValues[0]
                    } else {
                        val valueContext = context.toValueContext(this, value, attValues[0])
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
            RequestContext.doWithCtx({ it.withSourceIdMapping(emptyMap()) }) {
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
        private val valueRef: RecordRef,
        val ctxSourceId: String,
        val context: RequestContext?,
        val computedAtts: Map<String, RecordComputedAtt>
    ) {

        companion object {
            val EMPTY = ValueContext(
                null,
                null,
                null,
                EmptyAttValue.INSTANCE,
                RecordRef.EMPTY,
                "",
                null, emptyMap()
            )
        }

        private val computedRawRef: RecordRef by lazy {
            var result = if (RecordRef.isNotEmpty(valueRef)) {
                valueRef
            } else {
                val id = value.id
                var computedRef: RecordRef = if (id == null || id is String && StringUtils.isBlank(id)) {
                    if (resolveCtx != null && ID_SCALARS_SCHEMA.contains(resolveCtx.path)) {
                        RecordRef.create(ctxSourceId, UUID.randomUUID().toString())
                    } else {
                        RecordRef.EMPTY
                    }
                } else if (id is RecordRef) {
                    id
                } else if (id is EntityRef) {
                    RecordRef.valueOf(id)
                } else if (id is DataValue) {
                    RecordRef.create(ctxSourceId, id.asText())
                } else if (id is String) {
                    if (id.contains(RecordRef.SOURCE_DELIMITER)) {
                        RecordRef.valueOf(id)
                    } else {
                        RecordRef.create(ctxSourceId, id)
                    }
                } else {
                    RecordRef.create(ctxSourceId, id.toString())
                }
                if (ctxSourceId.isNotEmpty() &&
                    computedRef.appName.isEmpty() &&
                    computedRef.sourceId.isEmpty() &&
                    computedRef.id.isNotEmpty()
                ) {
                    computedRef = RecordRef.create(ctxSourceId, computedRef.id)
                }
                computedRef
            }
            if (RecordRef.isEmpty(result)) {
                result
            } else {
                val currentAppName = resolver?.currentAppName ?: ""
                if (result.appName.isBlank() && currentAppName.isNotBlank()) {
                    result = result.withDefaultAppName(currentAppName)
                }
                result
            }
        }

        private val computedRef: RecordRef by lazy {
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
            return if (RecordRef.isNotEmpty(valueRef)) {
                valueRef
            } else {
                try {
                    value.id
                } catch (e: Throwable) {
                    log.error(e) { "value.getId throws exception" }
                    "ERROR"
                }
            }.toString()
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
            val isLocalIdSchema = attribute == ScalarType.LOCAL_ID.schema
            // special case for ?localId because it is not equal to any other scalars
            if (value is AttValueProxy && (isLocalIdSchema || !attribute.startsWith('?'))) {
                val res = value.getAtt(attribute)
                return if (isLocalIdSchema) {
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
                    RecordConstants.ATT_ECOS_TYPE -> RecTypeUtils.anyTypeToRef(value.type)
                    RecordConstants.ATT_AS -> AttFuncValue { type -> value.getAs(type) }
                    RecordConstants.ATT_HAS -> AttFuncValue { name -> value.has(name) }
                    RecordConstants.ATT_EDGE -> AttFuncValue { name -> AttEdgeValue(value.getEdge(name)) }
                    RecordConstants.ATT_SELF -> value
                    else -> {
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

        fun getRef() = computedRef

        fun getRawRef() = computedRawRef

        fun getLocalId() = getRawRef().getLocalId()

        private fun getScalar(scalar: ScalarType, attribute: String): Any? {
            return when (scalar) {
                ScalarType.STR -> value.asText()
                ScalarType.DISP -> {
                    val disp = value.displayName

                    if (disp == null || LibsUtils.isJacksonPresent() && disp is JackNullNode) {
                        null
                    } else if (disp is DataValue && disp.isNull()) {
                        null
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
        val converter: AttValuesConverter,
        val mixinCtx: MixinContext,
        val recordTypeService: RecordTypeService
    ) {
        val attContext: AttContext = AttContext.getCurrentNotNull()
        val reqContext: RequestContext = RequestContext.getCurrentNotNull()
        val disabledMixinPaths: MutableSet<String> = HashSet()
        val disabledComputedPaths: MutableSet<String> = HashSet()
        var path: String = ""
        var rootValue: ValueContext = ValueContext.EMPTY

        private fun convertToAttValue(value: Any): AttValue? {
            val result = if (value is AttValue) {
                value
            } else {
                converter.toAttValue(value)
            }
            // At this moment we will wait until async initialization will be completed
            // in this method, but in future this waiting may be moved outside for various optimizations
            result?.init()?.get()
            return result
        }

        fun toRootValueContext(resolver: AttSchemaResolver, value: Any, valueRef: RecordRef): ValueContext {
            val attValue = convertToAttValue(value) ?: return ValueContext.EMPTY
            return ValueContext(
                resolver,
                this,
                null,
                attValue,
                valueRef,
                reqContext.getVar(CTX_SOURCE_ID_KEY)
                    ?: "",
                reqContext,
                getComputedAtts(null, attValue)
            )
        }

        fun toValueContext(resolver: AttSchemaResolver, parent: ValueContext?, value: Any?): ValueContext {
            if (value == null) {
                return ValueContext.EMPTY
            }
            val attValue = convertToAttValue(value) ?: return ValueContext.EMPTY
            return ValueContext(
                resolver,
                this,
                parent,
                attValue,
                RecordRef.EMPTY,
                reqContext.getVar(CTX_SOURCE_ID_KEY)
                    ?: "",
                reqContext,
                getComputedAtts(parent, attValue)
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
                if (RecordRef.isNotEmpty(typeRef)) {
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

        override fun getValue(): Any {
            return valueCtx.value
        }

        override fun getRef(): RecordRef {
            return valueCtx.getRef()
        }

        override fun getRawRef(): RecordRef {
            return valueCtx.getRawRef()
        }

        override fun getLocalId(): String {
            return valueCtx.getLocalId()
        }

        override fun getAtt(attribute: String): DataValue {
            return getAtts(Collections.singletonMap("k", attribute)).get("k")
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
}
