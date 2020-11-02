package ru.citeck.ecos.records3.record.op.atts.service

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.op.atts.service.mixin.AttMixin
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt
import ru.citeck.ecos.records3.record.op.atts.service.schema.resolver.ResolveArgs
import java.util.*
import java.util.function.Consumer
import kotlin.collections.Collection
import kotlin.collections.LinkedHashMap
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.emptyList
import kotlin.collections.indices
import kotlin.collections.listOf

class RecordAttsServiceImpl(services: RecordsServiceFactory) : RecordAttsService {

    companion object {
        private const val REF_ATT_ALIAS: String = "___ref___"
    }

    private val schemaReader = services.attSchemaReader
    private val dtoSchemaReader = services.dtoSchemaReader
    private val schemaResolver = services.attSchemaResolver

    override fun <T : Any> getAtts(value: Any?, attributes: Class<T>): T {
        return getAtts(listOf(value), attributes)[0]
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
        val attsMap = dtoSchemaReader.read(attributes)
        return getAtts(values, attsMap, false, emptyList())
            .map { dtoSchemaReader.instantiate(attributes, it.getAtts()) ?: attributes.newInstance() }
    }

    override fun getAtts(values: List<*>, attributes: Map<String, String>, rawAtts: Boolean): List<RecordAtts> {
        return getAtts(values, attributes, rawAtts, emptyList())
    }

    override fun getAtts(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        mixins: List<AttMixin>
    ): List<RecordAtts> {
        return getAtts(values, attributes, rawAtts, mixins, emptyList())
    }

    override fun getAtts(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        mixins: List<AttMixin>,
        recordRefs: List<RecordRef>
    ): List<RecordAtts> {

        var rootAtts: List<SchemaAtt> = attributes
        val valueRefsProvided = recordRefs.size == values.size
        rootAtts = ArrayList(rootAtts)

        if (!valueRefsProvided) {
            rootAtts.add(
                SchemaAtt.create()
                    .withAlias(REF_ATT_ALIAS)
                    .withName("?id")
                    .build()
            )
        }

        val data: List<Map<String, Any?>> = schemaResolver.resolve(
            ResolveArgs
                .create()
                .withValues(values)
                .withAttributes(rootAtts)
                .withRawAtts(rawAtts)
                .withMixins(mixins)
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
        mixins: List<AttMixin>
    ): List<RecordAtts> {
        return getAtts(values, schemaReader.read(attributes), rawAtts, mixins)
    }

    private fun toRecAtts(data: Map<String, Any?>, id: RecordRef): RecordAtts {
        var data = data
        var id: RecordRef = id
        if (id === RecordRef.EMPTY) {
            val alias = data[REF_ATT_ALIAS]
            id = if (alias == null) {
                RecordRef.EMPTY
            } else {
                RecordRef.valueOf(alias.toString())
            }
            if (StringUtils.isBlank(id.id)) {
                id = RecordRef.create(id.appName, id.sourceId, UUID.randomUUID().toString())
            }
            data = LinkedHashMap(data)
            data.remove(REF_ATT_ALIAS)
        }
        return RecordAtts(id, ObjectData.create(data))
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
