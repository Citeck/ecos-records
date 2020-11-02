package ru.citeck.ecos.records3.record.op.atts.service.value.factory

import ecos.com.fasterxml.jackson210.databind.JsonNode
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt
import ru.citeck.ecos.records3.record.op.atts.service.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.InnerAttValue
import java.util.*

class RecordRefValueFactory(services: RecordsServiceFactory) : AttValueFactory<RecordRef> {

    companion object {
        private val ATT_ID: String = "?id"
        private val ATT_LOCAL_ID: String = "?localId"
        private val ATT_STR: String = "?str"
        private val ATT_ASSOC: String = "?assoc"

        val ATTS_WITHOUT_LOADING = setOf(
            ATT_ID,
            ATT_LOCAL_ID,
            ATT_STR,
            ATT_ASSOC
        )
    }

    private val recordsService = services.recordsServiceV1
    private val schemaWriter = services.attSchemaWriter

    override fun getValue(value: RecordRef): AttValue? {
        return RecordRefValue(value)
    }

    override fun getValueTypes() = listOf(RecordRef::class.java)

    inner class RecordRefValue(private val ref: RecordRef) : AttValue {

        private val innerAtts: InnerAttValue

        init {
            val schemaAtt: SchemaAtt = AttContext.getCurrentSchemaAtt()
            val innerSchema = schemaAtt.inner

            val attsMap: MutableMap<String, String> = HashMap()
            val sb = StringBuilder()
            for (inner in innerSchema) {
                val innerName: String = inner.name
                if (!ATTS_WITHOUT_LOADING.contains(innerName)) {
                    schemaWriter.write(inner, sb)
                    attsMap[inner.name] = sb.toString()
                    sb.setLength(0)
                }
            }
            val atts: RecordAtts
            if (attsMap.isNotEmpty()) {
                atts = recordsService.getAtts(setOf(ref), attsMap, true)[0]
            } else {
                atts = RecordAtts(ref)
            }

            var dataNode: JsonNode = atts.getAtts().getData().asJson()
            dataNode = schemaWriter.unescapeKeys(dataNode)
            innerAtts = InnerAttValue(dataNode)
        }

        override fun getId(): RecordRef {
            return ref
        }

        override fun asText(): String? {
            return ref.toString()
        }

        override fun getDisplayName(): String? {
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
            return innerAtts.getAtt(name)
        }
    }
}
