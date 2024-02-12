package ru.citeck.ecos.records3.record.resolver.interceptor

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
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

    private val context = services.micrometerContext
    private val attSchemaWriter = services.attSchemaWriter

    override fun queryRecords(
        queryArg: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: QueryRecordsInterceptorsChain
    ): RecsQueryRes<RecordAtts> {

        return context.createObservation("ecos.records.query")
            .lowCardinalityKeyValue(SOURCE_ID, queryArg.sourceId)
            .highCardinalityKeyValue(ATTS_TO_LOAD) { formatAttsToLoad(attributes) }
            .observe { chain.invoke(queryArg, attributes, rawAtts) }
    }

    override fun getRecordsAtts(
        sourceId: String,
        recordIds: List<String>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: GetRecordsAttsInterceptorsChain
    ): List<RecordAtts> {

        return context.createObservation("ecos.records.get-atts")
            .lowCardinalityKeyValue(SOURCE_ID) { sourceId }
            .highCardinalityKeyValues {
                listOf(
                    RECORD_IDS to recordIds.joinToString(","),
                    ATTS_TO_LOAD to formatAttsToLoad(attributes)
                )
            }
            .observe { chain.invoke(sourceId, recordIds, attributes, rawAtts) }
    }

    private fun formatAttsToLoad(attributes: List<SchemaAtt>): String {
        if (attributes.isEmpty()) {
            return "[]"
        }
        val resArr = DataValue.createArr()
        attributes.forEach {
            resArr.add(DataValue.createStr(attSchemaWriter.write(it)))
        }
        return resArr.toString()
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

        return context.createObservation("ecos.records.mutate")
            .lowCardinalityKeyValue(SOURCE_ID, sourceId)
            .highCardinalityKeyValues {
                listOf(
                    RECORD_ID to record.id,
                    RECORD_ATTS to formatMutatedRecordAtts(record.getAtts()),
                    ATTS_TO_LOAD to formatAttsToLoad(attsToLoad)
                )
            }.observe { chain.invoke(sourceId, record, attsToLoad, rawAtts) }
    }

    private fun formatMutatedRecordAtts(atts: ObjectData): String {
        val attsStr = StringBuilder()
        val attsIt = atts.fieldNames()
        while (attsIt.hasNext()) {
            attsStr.append(attsIt.next()).append(",")
        }
        if (attsStr.isNotEmpty()) {
            attsStr.setLength(attsStr.length - 1)
        }
        return attsStr.toString()
    }

    override fun deleteRecords(
        sourceId: String,
        recordIds: List<String>,
        chain: DeleteRecordsInterceptorsChain
    ): List<DelStatus> {

        return context.createObservation("ecos.records.delete")
            .lowCardinalityKeyValue(SOURCE_ID, sourceId)
            .highCardinalityKeyValue(RECORD_IDS) { recordIds.joinToString(",") }
            .observe { chain.invoke(sourceId, recordIds) }
    }
}
