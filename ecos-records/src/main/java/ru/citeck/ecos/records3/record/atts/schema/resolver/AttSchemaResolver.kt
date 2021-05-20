package ru.citeck.ecos.records3.record.atts.schema.resolver

import ecos.com.fasterxml.jackson210.databind.JsonNode
import ecos.com.fasterxml.jackson210.databind.node.NullNode
import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.LibsUtils
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.meta.util.AttStrUtils
import ru.citeck.ecos.records2.request.error.ErrorUtils
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.computed.ComputedAtt
import ru.citeck.ecos.records3.record.atts.proc.AttProcDef
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.atts.value.AttValuesConverter
import ru.citeck.ecos.records3.record.atts.value.HasListView
import ru.citeck.ecos.records3.record.atts.value.impl.AttEdgeValue
import ru.citeck.ecos.records3.record.atts.value.impl.AttFuncValue
import ru.citeck.ecos.records3.record.atts.value.impl.EmptyAttValue
import ru.citeck.ecos.records3.record.mixin.MixinContext
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import ru.citeck.ecos.records3.record.type.RecordTypeService
import ru.citeck.ecos.records3.utils.AttUtils
import java.lang.reflect.Array
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap
import com.fasterxml.jackson.databind.node.NullNode as JackNullNode

class AttSchemaResolver(private val factory: RecordsServiceFactory) {

    companion object {
        val log = KotlinLogging.logger {}

        const val CTX_SOURCE_ID_KEY: String = "ctx-source-id"
    }

    private val attValuesConverter = factory.attValuesConverter
    private val attProcService = factory.attProcService
    private val attSchemaReader = factory.attSchemaReader
    private val dtoSchemaReader = factory.dtoSchemaReader
    private val computedAttsService = factory.computedAttsService

    private val recordTypeService by lazy { factory.recordTypeService }

    fun resolve(args: ResolveArgs): List<Map<String, Any?>> {
        val context = AttContext.getCurrent()
        return if (context == null) {
            AttContext.doWithCtx(factory) { resolveInAttCtx(args) }
        } else {
            resolveInAttCtx(args)
        }
    }

    private fun resolveInAttCtx(args: ResolveArgs): List<Map<String, Any?>> {

        val values: List<Any?> = args.values
        var schemaAttsToLoad: List<SchemaAtt> = args.attributes

        if (!args.rawAtts) {
            val processorsAtts: List<SchemaAtt> = attProcService.getProcessorsAtts(schemaAttsToLoad)
            if (processorsAtts.isNotEmpty()) {
                schemaAttsToLoad = ArrayList(schemaAttsToLoad)
                schemaAttsToLoad.addAll(processorsAtts)
            }
        }
        val schemaAtts = schemaAttsToLoad
        val context = ResolveContext(attValuesConverter, args.mixinCtx, recordTypeService)
        val attValues = ArrayList<ValueContext>()
        for (i in values.indices) {
            val ref = if (args.valueRefs.isEmpty()) {
                RecordRef.EMPTY
            } else {
                args.valueRefs[i]
            }
            attValues.add(context.toRootValueContext(values[i] ?: EmptyAttValue.INSTANCE, ref))
        }
        val simpleAtts = AttSchemaUtils.simplifySchema(schemaAtts)
        val result = resolveRoot(attValues, simpleAtts, context)
        return resolveResultsWithAliases(result, schemaAtts, args.rawAtts)
    }

    private fun resolveResultsWithAliases(
        values: List<Map<String, Any?>>,
        atts: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<Map<String, Any?>> {
        if (atts.isEmpty()) {
            return values.map { emptyMap<String, Any?>() }
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
            attProcService.applyProcessors(result, processors)
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

            var attValue: Any?
            var attName = att.name

            if (currentValuePath.isEmpty() && att.name.startsWith("$")) {

                val contextAttName = att.name.substring(1)
                attValue = context.reqContext.ctxData.ctxAtts[contextAttName]
            } else {

                if (attName.length > 2 && attName[0] == '\\' && attName[1] == '$') {
                    attName = attName.substring(1)
                }

                val computedAtts: Map<String, ComputedAtt> = value.computedAtts
                val computedAtt: ComputedAtt? = computedAtts[attName]

                if (computedAtt != null && disabledComputedPaths.add(attPath)) {
                    attValue = try {
                        val valueCtx = getContextForDynamicAtt(value, computedAtt.id)
                        if (valueCtx != null) {
                            computedAttsService.compute(
                                AttValueResolveCtx(
                                    currentValuePath,
                                    context,
                                    valueCtx
                                ),
                                computedAtt
                            )
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
                                mixinAttCtx.mixin.getAtt(
                                    mixinAttCtx.path,
                                    AttValueResolveCtx(
                                        currentValuePath,
                                        context,
                                        mixinValueCtx
                                    )
                                )
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
            val attValues = toList(attValue)
            val alias = att.getAliasForValue()
            if (att.multiple) {
                val values: List<ValueContext> = attValues.stream()
                    .map { v: Any? -> context.toValueContext(value, v) }
                    .collect(Collectors.toList())
                result[alias] = resolve(values, att.inner, context)
            } else {
                if (attValues.isEmpty()) {
                    result[alias] = null
                } else {
                    if (att.isScalar()) {
                        result[alias] = attValues[0]
                    } else {
                        val valueContext = context.toValueContext(value, attValues[0])
                        result[alias] = resolve(valueContext, att.inner, context)
                    }
                }
            }
        }
        context.path = currentValuePath
        attContext.setSchemaAtt(currentSchemaAtt)

        return result
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

    private fun toList(rawValue: Any?): List<Any> {

        return if (rawValue == null || isNull(rawValue)) {
            emptyList()
        } else if (rawValue is HasListView<*>) {
            ArrayList(rawValue.getListView())
        } else if (rawValue is DataValue) {
            if (rawValue.isArray()) {
                rawValue.toList()
            } else {
                arrayListOf(rawValue)
            }
        } else if (rawValue is Collection<*>) {
            ArrayList(rawValue.filterNotNull())
        } else if (rawValue.javaClass.isArray) {
            if (ByteArray::class.java == rawValue.javaClass) {
                listOf(rawValue)
            } else {
                val length = Array.getLength(rawValue)
                if (length == 0) {
                    emptyList()
                } else {
                    val result = ArrayList<Any>(length)
                    for (i in 0 until length) {
                        result.add(Array.get(rawValue, i))
                    }
                    result
                }
            }
        } else {
            listOf(rawValue)
        }
    }

    private fun isNull(rawValue: Any?): Boolean {

        if (rawValue == null || rawValue is RecordRef && RecordRef.isEmpty(rawValue) ||
            rawValue is DataValue && rawValue.isNull()
        ) {

            return true
        }
        if (rawValue is JsonNode) {
            return rawValue.isNull || rawValue.isMissingNode
        }
        if (LibsUtils.isJacksonPresent()) {
            if (rawValue is com.fasterxml.jackson.databind.JsonNode) {
                return rawValue.isNull || rawValue.isMissingNode
            }
        }
        return false
    }

    private class ValueContext(
        val parent: ValueContext?,
        val value: AttValue,
        private val valueRef: RecordRef,
        val ctxSourceId: String,
        val context: RequestContext?,
        val computedAtts: Map<String, ComputedAtt>
    ) {

        companion object {
            val EMPTY = ValueContext(
                null,
                EmptyAttValue.INSTANCE,
                RecordRef.EMPTY,
                "",
                null, emptyMap()
            )
        }

        private val computedRef: RecordRef by lazy {
            if (RecordRef.isNotEmpty(valueRef)) {
                valueRef
            } else {
                val id = value.id
                var computedRef = if (id == null || id is String && StringUtils.isBlank(id)) {
                    RecordRef.create(ctxSourceId, UUID.randomUUID().toString())
                } else if (id is RecordRef) {
                    id
                } else if (id is DataValue) {
                    RecordRef.create(ctxSourceId, id.asText())
                } else if (id is String) {
                    val sourceDelimIdx = id.indexOf(RecordRef.SOURCE_DELIMITER)
                    val appNameDelimIdx = id.indexOf(RecordRef.APP_NAME_DELIMITER)
                    if (sourceDelimIdx > 0 && appNameDelimIdx > 0 && appNameDelimIdx < sourceDelimIdx) {
                        RecordRef.valueOf(id)
                    } else {
                        RecordRef.create(ctxSourceId, id)
                    }
                } else {
                    RecordRef.create(ctxSourceId, id.toString())
                }
                if (ctxSourceId.isNotEmpty() &&
                    computedRef.appName.isEmpty() &&
                    computedRef.sourceId.isEmpty()
                ) {
                    computedRef = RecordRef.create(ctxSourceId, computedRef.id)
                }
                computedRef
            }
        }

        fun resolve(attContext: AttContext): Any? {

            val schemaAtt = attContext.getSchemaAtt()
            val name = schemaAtt.name

            if (log.isTraceEnabled) {
                log.trace("Resolve $schemaAtt")
            }

            val res: Any?

            res = try {
                resolveImpl(name)
            } catch (e: Throwable) {
                log.error {
                    "Attribute resolving error. Attribute: $name Value type: ${value::class.qualifiedName} " +
                        "Message: ${e.message} RequestId: ${context?.ctxData?.requestId}"
                }
                if (context == null || !context.ctxData.omitErrors) {
                    throw e
                }
                context.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e) }
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
            val scalarType = ScalarType.getBySchemaOrMirrorAtt(attribute)
            return if (scalarType != null) {
                getScalar(scalarType)
            } else {
                when (attribute) {
                    RecordConstants.ATT_TYPE,
                    RecordConstants.ATT_ECOS_TYPE -> value.type
                    RecordConstants.ATT_AS -> AttFuncValue { type -> value.getAs(type) }
                    RecordConstants.ATT_HAS -> AttFuncValue { name -> value.has(name) }
                    RecordConstants.ATT_EDGE -> AttFuncValue { name -> AttEdgeValue(value.getEdge(name)) }
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

        fun getLocalId() = getRef().id

        @Throws(Exception::class)
        private fun getScalar(scalar: ScalarType): Any? {
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
                            is MLText -> MLText.getClosestValue(disp, RequestContext.getLocale())
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
            return if (value is AttValue) {
                value
            } else {
                converter.toAttValue(value)
            }
        }

        fun toRootValueContext(value: Any, valueRef: RecordRef): ValueContext {
            val attValue = convertToAttValue(value) ?: return ValueContext.EMPTY
            return ValueContext(
                null,
                attValue,
                valueRef,
                reqContext.getVar(CTX_SOURCE_ID_KEY)
                    ?: "",
                reqContext,
                getComputedAtts(null, attValue)
            )
        }

        fun toValueContext(parent: ValueContext?, value: Any?): ValueContext {
            if (value == null) {
                return ValueContext.EMPTY
            }
            val attValue = convertToAttValue(value) ?: return ValueContext.EMPTY
            return ValueContext(
                parent,
                attValue,
                RecordRef.EMPTY,
                reqContext.getVar(CTX_SOURCE_ID_KEY)
                    ?: "",
                reqContext,
                getComputedAtts(parent, attValue)
            )
        }

        private fun getComputedAtts(parent: ValueContext?, value: AttValue?): Map<String, ComputedAtt> {
            val computedAtts = HashMap<String, ComputedAtt>()
            parent?.computedAtts?.forEach { (id, att) ->
                val dotIdx: Int = AttStrUtils.indexOf(id, ".")
                if (dotIdx > 0 && dotIdx < id.length + 1) {
                    computedAtts[id.substring(dotIdx + 1)] = att
                }
            }
            try {
                val typeRef = value?.type ?: RecordRef.EMPTY
                if (RecordRef.isNotEmpty(typeRef)) {
                    for (att in recordTypeService.getComputedAtts(typeRef)) {
                        if (AttUtils.isValidComputedAtt(att.id)) {
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

        override fun getRef(): RecordRef {
            return valueCtx.getRef()
        }

        override fun getLocalId(): String {
            return valueCtx.getLocalId()
        }

        override fun getAtt(attribute: String): DataValue {
            return getAtts(Collections.singletonMap("k", attribute)).get("k")
        }

        override fun <T : Any> getAtts(attributes: Class<T>): T {
            val schema = dtoSchemaReader.read(attributes)
            return dtoSchemaReader.instantiate(attributes, getAttsBySchema(schema)) ?: attributes.newInstance()
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
