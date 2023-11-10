package ru.citeck.ecos.records3.record.atts.value.factory

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.atts.schema.write.AttSchemaWriter
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.AttValueProxy
import ru.citeck.ecos.records3.record.atts.value.impl.InnerAttValue
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.promise.Promise
import kotlin.collections.LinkedHashMap

class RecordRefValueFactory : AttValueFactory<RecordRef>, ServiceFactoryAware {

    companion object {
        private val SCALARS_WITHOUT_LOADING = listOf(
            ScalarType.ID,
            ScalarType.LOCAL_ID,
            ScalarType.APP_NAME,
            ScalarType.ASSOC,
            ScalarType.STR,
            ScalarType.RAW
        )
        private val ATTS_WITHOUT_LOADING = setOf(
            *SCALARS_WITHOUT_LOADING.map { it.schema }.toTypedArray(),
            *SCALARS_WITHOUT_LOADING.map { it.mirrorAtt }.toTypedArray()
        )

        @JvmStatic
        fun getAttsToLoad(innerSchema: List<SchemaAtt>, schemaWriter: AttSchemaWriter): Map<String, String> {

            val attsMap: MutableMap<String, String> = LinkedHashMap()
            val sb = StringBuilder()

            for (inner in innerSchema) {

                val innerName: String = inner.name

                if (!innerName.startsWith('$') && !ATTS_WITHOUT_LOADING.contains(innerName)) {
                    schemaWriter.write(inner, sb, false)
                    attsMap[innerName] = sb.toString()
                    sb.setLength(0)
                }
            }

            return attsMap
        }
    }

    private lateinit var recordsService: RecordsService
    private lateinit var schemaWriter: AttSchemaWriter

    override fun getValue(value: RecordRef): AttValue {
        return RecordRefValue(value)
    }

    override fun getValueTypes() = listOf(RecordRef::class.java)

    override fun setRecordsServiceFactory(serviceFactory: RecordsServiceFactory) {
        this.recordsService = serviceFactory.recordsServiceV1
        this.schemaWriter = serviceFactory.attSchemaWriter
    }

    inner class RecordRefValue(private val ref: RecordRef) : AttValue, AttValueProxy {

        private lateinit var innerAtts: InnerAttValue

        fun init(loadedAtts: ObjectData) {
            innerAtts = InnerAttValue(loadedAtts.getData().asJson())
        }

        override fun init(): Promise<Unit>? {
            val attsToLoad = getAttsToLoad(AttContext.getCurrentSchemaAtt().inner, schemaWriter)
            val atts = if (attsToLoad.isNotEmpty()) {
                loadRawAtts(attsToLoad).getAtts()
            } else {
                ObjectData.create()
            }
            innerAtts = InnerAttValue(atts.getData().asJson())
            return null
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
                    ScalarType.APP_NAME -> id.appName
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

        fun getRef(): EntityRef {
            return ref
        }
    }
}
