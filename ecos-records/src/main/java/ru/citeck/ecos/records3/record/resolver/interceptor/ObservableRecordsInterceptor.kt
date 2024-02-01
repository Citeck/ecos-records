package ru.citeck.ecos.records3.record.resolver.interceptor

import io.micrometer.observation.Observation
import ru.citeck.ecos.micrometer.observeKt
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import java.lang.StringBuilder

open class ObservableRecordsInterceptor(services: RecordsServiceFactory) : LocalRecordsInterceptor {

    companion object {
        private const val SOURCE_ID = "sourceId"
        private const val RECORD_ID = "recordId"
        private const val RECORD_ATTS = "recordAtts"
        private const val ATTS_TO_LOAD = "attsToLoad"
        private const val RECORD_IDS = "recordIds"
    }

    private val registry = services.micrometerContext.getObservationRegistry()

    open fun isValid(): Boolean {
        return !registry.isNoop
    }

    override fun queryRecords(
        queryArg: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: QueryRecordsInterceptorsChain
    ): RecsQueryRes<RecordAtts> {

        return Observation.createNotStarted("ecos.records.query", registry)
            .lowCardinalityKeyValue(SOURCE_ID, queryArg.sourceId)
            .highCardinalityKeyValue(ATTS_TO_LOAD, formatAttributes(attributes))
            .observeKt { chain.invoke(queryArg, attributes, rawAtts) }
    }

    override fun getRecordsAtts(
        sourceId: String,
        recordIds: List<String>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: GetRecordsAttsInterceptorsChain
    ): List<RecordAtts> {

        return Observation.createNotStarted("ecos.records.get-atts", registry)
            .lowCardinalityKeyValue(SOURCE_ID, sourceId)
            .highCardinalityKeyValue(RECORD_IDS, recordIds.joinToString(","))
            .highCardinalityKeyValue(ATTS_TO_LOAD, formatAttributes(attributes))
            .observeKt { chain.invoke(sourceId, recordIds, attributes, rawAtts) }
    }

    private fun formatAttributes(attributes: List<SchemaAtt>): String {
        val atts = StringBuilder()
        attributes.forEach {
            atts.append(it.name).append(",")
        }
        if (atts.isNotEmpty()) {
            atts.setLength(atts.length - 1)
        }
        return atts.toString()
    }

    override fun getValuesAtts(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: GetValuesAttsInterceptorsChain
    ): List<RecordAtts> {
        return chain.invoke(values, attributes, rawAtts)
    }

    override fun mutateRecord(
        sourceId: String,
        record: LocalRecordAtts,
        attsToLoad: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: MutateRecordInterceptorsChain
    ): RecordAtts {

        val atts = StringBuilder()
        val attsIt = record.getAtts().fieldNames()
        while (attsIt.hasNext()) {
            atts.append(attsIt.next()).append(",")
        }
        if (atts.isNotEmpty()) {
            atts.setLength(atts.length - 1)
        }

        return Observation.createNotStarted("ecos.records.mutate", registry)
            .lowCardinalityKeyValue(SOURCE_ID, sourceId)
            .highCardinalityKeyValue(RECORD_ID, record.id)
            .highCardinalityKeyValue(RECORD_ATTS, atts.toString())
            .highCardinalityKeyValue(ATTS_TO_LOAD, formatAttributes(attsToLoad))
            .observeKt { chain.invoke(sourceId, record, attsToLoad, rawAtts) }
    }

    override fun deleteRecords(
        sourceId: String,
        recordIds: List<String>,
        chain: DeleteRecordsInterceptorsChain
    ): List<DelStatus> {

        return Observation.createNotStarted("ecos.records.delete", registry)
            .lowCardinalityKeyValue(SOURCE_ID, sourceId)
            .highCardinalityKeyValue(RECORD_IDS, recordIds.joinToString(","))
            .observeKt { chain.invoke(sourceId, recordIds) }
    }
}
