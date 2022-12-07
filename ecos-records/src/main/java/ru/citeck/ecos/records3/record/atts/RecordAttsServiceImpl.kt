package ru.citeck.ecos.records3.record.atts

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.atts.schema.resolver.ResolveArgs
import ru.citeck.ecos.records3.record.mixin.EmptyMixinContext
import ru.citeck.ecos.records3.record.mixin.MixinContext
import java.util.*
import java.util.function.Consumer
import kotlin.collections.Collection
import kotlin.collections.LinkedHashMap
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.emptyList
import kotlin.collections.indices
import kotlin.collections.listOf

class RecordAttsServiceImpl(private val services: RecordsServiceFactory) : RecordAttsService {

    companion object {
        private const val REF_ATT_ALIAS: String = "___ref___"
        private val REF_ATT = SchemaAtt.create()
            .withAlias(REF_ATT_ALIAS)
            .withName("?id")
            .build()
    }

    private val schemaReader = services.attSchemaReader
    private val dtoSchemaReader = services.dtoSchemaReader
    private val schemaResolver = services.attSchemaResolver

    override fun <T> doWithSchema(attributes: Map<String, *>, rawAtts: Boolean, action: (List<SchemaAtt>) -> T): T {
        return doWithSchema(schemaReader.read(attributes), rawAtts, action)
    }

    override fun <T> doWithSchema(atts: List<SchemaAtt>, rawAtts: Boolean, action: (List<SchemaAtt>) -> T): T {
        return AttContext.doWithCtx(services) { attContext ->
            val schemaAttBefore = attContext.getSchemaAtt()
            val attPathBefore = attContext.getAttPath()
            if (atts.isNotEmpty()) {
                val flatAtts = schemaResolver.getFlatAttributes(atts, !rawAtts)
                attContext.setSchemaAtt(
                    SchemaAtt.create()
                        .withName(SchemaAtt.ROOT_NAME)
                        .withInner(flatAtts)
                        .build()
                )
                attContext.setAttPath("")
            }
            try {
                action.invoke(atts)
            } finally {
                attContext.setSchemaAtt(schemaAttBefore)
                attContext.setAttPath(attPathBefore)
            }
        }
    }

    override fun <T : Any> getAtts(value: Any?, attributes: Class<T>): T {
        return getAtts(listOf(value), attributes)[0]
    }

    override fun getId(value: Any?, defaultRef: RecordRef): RecordRef {

        value ?: return defaultRef

        return when (value) {
            is RecordRef -> value
            is RecordAtts -> value.getId()
            is String -> {
                val ref = RecordRef.valueOf(value)
                if (ref.isEmpty()) {
                    return defaultRef
                }
                return ref.withDefault(
                    appName = defaultRef.appName,
                    sourceId = defaultRef.sourceId
                )
            }
            else -> {
                val atts = getAtts(listOf(value), listOf(REF_ATT), false, EmptyMixinContext, emptyList())
                val strId = atts[0].getStringOrNull(REF_ATT_ALIAS) ?: return defaultRef
                val result = RecordRef.valueOf(strId)
                if (RecordRef.isEmpty(result)) {
                    defaultRef
                } else {
                    result
                }
            }
        }
    }

    override fun getAtts(value: Any?, attributes: Map<String, String>): RecordAtts {
        return getAtts(value, attributes, false)
    }

    override fun getAtts(value: Any?, attributes: Collection<String>): RecordAtts {
        return getAtts(value, attributes, false)
    }

    override fun getAtts(value: Any?, attributes: Collection<String>, rawAtts: Boolean): RecordAtts {
        return getAtts(value, toAttsMap(attributes), rawAtts)
    }

    override fun getAtts(value: Any?, attributes: Map<String, String>, rawAtts: Boolean): RecordAtts {
        val values: List<Any?> = listOf(value)
        return getFirst(getAtts(values, attributes, rawAtts), attributes, values)
    }

    override fun getAtts(values: List<*>, attributes: Collection<String>): List<RecordAtts> {
        return getAtts(values, attributes, false)
    }

    override fun getAtts(values: List<*>, attributes: Map<String, String>): List<RecordAtts> {
        return getAtts(values, attributes, false)
    }

    override fun getAtts(values: List<*>, attributes: Collection<String>, rawAtts: Boolean): List<RecordAtts> {
        return getAtts(values, toAttsMap(attributes), rawAtts)
    }

    override fun <T : Any> getAtts(values: List<*>, attributes: Class<T>): List<T> {
        val schema = dtoSchemaReader.read(attributes)
        return getAtts(values, schema, false, EmptyMixinContext)
            .map {
                dtoSchemaReader.instantiate(attributes, it.getAtts())
                    ?: error("Attributes class can't be instantiated. Class: $attributes Schema: $schema")
            }
    }

    override fun getAtts(values: List<*>, attributes: Map<String, String>, rawAtts: Boolean): List<RecordAtts> {
        return getAtts(values, attributes, rawAtts, EmptyMixinContext)
    }

    override fun getAtts(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        mixins: MixinContext
    ): List<RecordAtts> {
        return getAtts(values, attributes, rawAtts, mixins, emptyList())
    }

    override fun getAtts(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        mixins: MixinContext,
        recordRefs: List<RecordRef>
    ): List<RecordAtts> {

        if (values.isEmpty()) {
            return emptyList()
        }

        var rootAtts: List<SchemaAtt> = attributes
        val valueRefsProvided = recordRefs.size == values.size
        rootAtts = ArrayList(rootAtts)

        if (!valueRefsProvided) {
            rootAtts.add(REF_ATT)
        }

        val data: List<Map<String, Any?>> = schemaResolver.resolve(
            ResolveArgs
                .create()
                .withValues(values)
                .withAttributes(rootAtts)
                .withRawAtts(rawAtts)
                .withMixinContext(mixins)
                .withValueRefs(recordRefs)
                .build()
        )

        val recordAtts = ArrayList<RecordAtts>()
        if (valueRefsProvided) {
            for (i in data.indices) {
                recordAtts.add(toRecAtts(data[i], recordRefs[i]))
            }
        } else {
            for (elem in data) {
                recordAtts.add(toRecAtts(elem, RecordRef.EMPTY))
            }
        }
        return recordAtts
    }

    override fun getAtts(
        values: List<*>,
        attributes: Map<String, String>,
        rawAtts: Boolean,
        mixins: MixinContext
    ): List<RecordAtts> {
        return getAtts(values, schemaReader.read(attributes), rawAtts, mixins)
    }

    private fun toRecAtts(data: Map<String, Any?>, id: RecordRef): RecordAtts {

        var resData = data
        var resId: RecordRef = id

        if (resId === RecordRef.EMPTY) {
            val alias = resData[REF_ATT_ALIAS]
            resId = if (alias == null) {
                RecordRef.EMPTY
            } else {
                RecordRef.valueOf(alias.toString())
            }
            if (StringUtils.isBlank(resId.id)) {
                resId = RecordRef.create(resId.appName, resId.sourceId, UUID.randomUUID().toString())
            }
            resData = LinkedHashMap(resData)
            resData.remove(REF_ATT_ALIAS)
        }
        return RecordAtts(resId, ObjectData.create(resData))
    }

    private fun <T> getFirst(elements: List<T>, atts: Any?, srcValues: List<Any?>): T {
        if (elements.isEmpty()) {
            throw RuntimeException(
                "Get atts returned 0 records. " +
                    "Attributes: " + atts + " Values: " + srcValues
            )
        }
        return elements[0]
    }

    private fun toAttsMap(attributes: Collection<String>): Map<String, String> {
        val attributesMap = LinkedHashMap<String, String>()
        attributes.forEach(Consumer { att: String -> attributesMap[att] = att })
        return attributesMap
    }
}
