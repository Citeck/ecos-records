package ru.citeck.ecos.records3.record.op.atts.service.value.factory

import ecos.com.fasterxml.jackson210.databind.JsonNode
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue
import ru.citeck.ecos.records3.record.op.atts.service.value.factory.RecordRefValueFactory
import java.util.*


class RecordRefValueFactory(serviceFactory: RecordsServiceFactory?) : AttValueFactory<RecordRef> {
    private val recordsService: RecordsService?
    private val schemaWriter: AttSchemaWriter? = AttSchemaGqlWriter()
    override fun getValue(value: RecordRef): AttValue? {
        return RecordRefValueFactory.RecordRefValue(value)
    }

    override val valueTypes: MutableList<Class<out T?>?>?
        get() = listOf(RecordRef::class.java)

    inner class RecordRefValue internal constructor(recordRef: RecordRef?) : AttValue {
        private val ref: RecordRef?
        private val innerAtts: InnerAttValue?
        override fun getId(): RecordRef? {
            return ref
        }

        override fun asText(): String? {
            return innerAtts.asText()
        }

        override fun getDisplayName(): String? {
            return innerAtts.getDispName()
        }

        override fun asDouble(): Double? {
            return innerAtts.asDouble()
        }

        override fun asBoolean(): Boolean? {
            return innerAtts.asBool()
        }

        override fun asJson(): Any? {
            return innerAtts.asJson()
        }

        override fun has(name: String): Boolean {
            return innerAtts.has(name)
        }

        override fun getAs(type: String): Any? {
            return innerAtts.`as`(type)
        }

        override fun getAtt(name: String): Any? {
            return innerAtts.getAtt(name)
        }

        fun getRef(): RecordRef? {
            return ref
        }

        init {
            val schemaAtt: SchemaAtt = AttContext.Companion.getCurrentSchemaAtt()
            val innerSchema: MutableList<SchemaAtt?> = schemaAtt.inner
            ref = recordRef
            val attsMap: MutableMap<String?, String?> = HashMap()
            val sb = StringBuilder()
            for (inner in innerSchema) {
                val innerName: String = inner.name
                if (innerName != RecordRefValueFactory.Companion.ATT_ID && innerName != RecordRefValueFactory.Companion.ATT_LOCAL_ID) {
                    schemaWriter.write(inner, sb)
                    attsMap[inner.name] = sb.toString()
                    sb.setLength(0)
                }
            }
            val atts: RecordAtts?
            if (attsMap.size > 0) {
                atts = recordsService.getAtts(setOf(ref), attsMap, true).get(0)
            } else {
                atts = RecordAtts(ref)
            }
            var dataNode: JsonNode = atts.getAtts().getData().asJson()
            dataNode = schemaWriter.unescapeKeys(dataNode)
            innerAtts = InnerAttValue(dataNode)

            //todo
            if (attsMap.containsKey("?assoc") && !attsMap.containsKey("?str")) {
                atts.setAtt("?str", atts.getStringOrNull("?assoc"))
            }
        }
    }

    companion object {
        private val ATT_ID: String? = "?id"
        private val ATT_LOCAL_ID: String? = "?localId"
    }

    init {
        recordsService = serviceFactory.getRecordsServiceV1()
    }
}
