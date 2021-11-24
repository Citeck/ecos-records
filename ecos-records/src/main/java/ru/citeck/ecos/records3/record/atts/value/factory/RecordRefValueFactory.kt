package ru.citeck.ecos.records3.record.atts.value.factory

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.AttValueProxy
import ru.citeck.ecos.records3.record.atts.value.impl.InnerAttValue
import kotlin.collections.LinkedHashMap

class RecordRefValueFactory(services: RecordsServiceFactory) : AttValueFactory<RecordRef> {

    companion object {
        private val SCALARS_WITHOUT_LOADING = listOf(
            ScalarType.ID,
            ScalarType.LOCAL_ID,
            ScalarType.ASSOC,
            ScalarType.STR,
            ScalarType.RAW
        )
        private val ATTS_WITHOUT_LOADING = setOf(
            *SCALARS_WITHOUT_LOADING.map { it.schema }.toTypedArray(),
            *SCALARS_WITHOUT_LOADING.map { it.mirrorAtt }.toTypedArray()
        )
    }

    private val recordsService = services.recordsServiceV1
    private val schemaWriter = services.attSchemaWriter

    override fun getValue(value: RecordRef): AttValue {
        return RecordRefValue(value)
    }

    override fun getValueTypes() = listOf(RecordRef::class.java)

    inner class RecordRefValue(private val ref: RecordRef) : AttValue, AttValueProxy {

        private val innerAtts: InnerAttValue

        init {
            val innerSchema = AttContext.getCurrentSchemaAtt().inner

            val attsMap: MutableMap<String, String> = LinkedHashMap()
            val sb = StringBuilder()

            for (inner in innerSchema) {

                val innerName: String = inner.name

                if (!ATTS_WITHOUT_LOADING.contains(innerName)) {
                    schemaWriter.write(inner, sb, false)
                    attsMap[innerName] = sb.toString()
                    sb.setLength(0)
                }
            }
            val atts = if (attsMap.isNotEmpty()) {
                loadRawAtts(attsMap)
            } else {
                RecordAtts(ref)
            }
            innerAtts = InnerAttValue(atts.getAtts().getData().asJson())
        }

        private fun loadRawAtts(attsMap: Map<String, String>): RecordAtts {
            return recordsService.getAtts(setOf(ref), attsMap, true)[0]
        }

        override fun getId(): RecordRef {
            return ref
        }

        override fun asText(): String {
            return ref.toString()
        }

        override fun getDisplayName(): Any? {
            return innerAtts.displayName
        }

        override fun asDouble(): Double? {
            return innerAtts.asDouble()
        }

        override fun asBoolean(): Boolean? {
            return innerAtts.asBoolean()
        }

        override fun asJson(): Any? {
            return innerAtts.asJson()
        }

        override fun has(name: String): Boolean {
            return innerAtts.has(name)
        }

        override fun getAs(type: String): Any? {
            return innerAtts.getAs(type)
        }

        override fun getAtt(name: String): Any? {
            if (ATTS_WITHOUT_LOADING.contains(name)) {
                val type = ScalarType.getBySchemaOrMirrorAtt(name) ?: return null
                return when (type) {
                    ScalarType.ID,
                    ScalarType.ASSOC,
                    ScalarType.STR,
                    ScalarType.RAW -> id
                    ScalarType.LOCAL_ID -> id.id
                    else -> null
                }
            }
            return innerAtts.getAtt(name)
        }

        override fun getType(): RecordRef {
            return innerAtts.type
        }

        override fun asRaw(): Any {
            return ref.toString()
        }
    }
}
