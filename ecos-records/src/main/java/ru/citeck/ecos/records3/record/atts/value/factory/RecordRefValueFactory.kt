package ru.citeck.ecos.records3.record.atts.value.factory

import ecos.com.fasterxml.jackson210.databind.JsonNode
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.impl.InnerAttValue
import java.util.*

class RecordRefValueFactory(services: RecordsServiceFactory) : AttValueFactory<RecordRef> {

    companion object {
        private const val ATT_ID: String = "?id"
        private const val ATT_LOCAL_ID: String = "?localId"
        private const val ATT_ASSOC: String = "?assoc"
        private const val ATT_STR: String = "?str"

        val ATTS_WITHOUT_LOADING = setOf(
            ATT_ID,
            ATT_LOCAL_ID,
            ATT_ASSOC,
            ATT_STR
        )
    }

    private val recordsService = services.recordsServiceV1
    private val schemaWriter = services.attSchemaWriter

    override fun getValue(value: RecordRef): AttValue {
        return RecordRefValue(value)
    }

    override fun getValueTypes() = listOf(RecordRef::class.java)

    inner class RecordRefValue(private val ref: RecordRef) : AttValue {

        private val innerAtts: InnerAttValue

        init {
            val schemaAtt: SchemaAtt = AttContext.getCurrentSchemaAtt()
            val innerSchema = schemaAtt.inner

            val scalarMirrorAtts = mutableListOf<ScalarType>()
            val attsMap: MutableMap<String, String> = LinkedHashMap()
            val sb = StringBuilder()

            for (inner in innerSchema) {

                val innerName: String = inner.name

                if (inner.name == RecordConstants.ATT_TYPE) {

                    attsMap["_type"] = "_type?id"
                } else if (!ATTS_WITHOUT_LOADING.contains(innerName)) {

                    schemaWriter.write(inner, sb, true)
                    val mirrorScalarType = ScalarType.getByMirrorAtt(innerName)
                    if (mirrorScalarType != null) {
                        scalarMirrorAtts.add(mirrorScalarType)
                    }
                    attsMap[innerName] = sb.toString()
                    sb.setLength(0)
                }
            }
            val atts = if (attsMap.isNotEmpty()) {
                val atts = recordsService.getAtts(setOf(ref), attsMap, true)[0]
                scalarMirrorAtts.forEach {
                    var scalarValue = atts.getAtt(it.mirrorAtt)
                    while (scalarValue.isObject()) {
                        scalarValue = if (scalarValue.size() > 0) {
                            scalarValue.get(scalarValue.fieldNamesList()[0])
                        } else {
                            DataValue.NULL
                        }
                    }
                    atts.setAtt(it.schema, scalarValue)
                }
                atts
            } else {
                RecordAtts(ref)
            }
            innerAtts = InnerAttValue(atts.getAtts().getData().asJson())
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
            return innerAtts.getAtt(name)
        }

        override fun getType(): RecordRef {
            return innerAtts.type
        }
    }
}
