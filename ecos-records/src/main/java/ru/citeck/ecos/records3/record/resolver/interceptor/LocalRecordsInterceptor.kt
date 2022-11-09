package ru.citeck.ecos.records3.record.resolver.interceptor

import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.resolver.LocalRecordsResolverImpl

interface LocalRecordsInterceptor {

    fun queryRecords(
        queryArg: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: QueryRecordsInterceptorsChain
    ): RecsQueryRes<RecordAtts>

    fun getValuesAtts(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: GetValuesAttsInterceptorsChain
    ): List<RecordAtts>

    fun getRecordsAtts(
        sourceId: String,
        recordIds: List<String>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: GetRecordsAttsInterceptorsChain
    ): List<RecordAtts>

    fun mutateRecord(
        sourceId: String,
        record: LocalRecordAtts,
        attsToLoad: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: MutateRecordInterceptorsChain
    ): RecordAtts

    fun deleteRecords(
        sourceId: String,
        recordIds: List<String>,
        chain: DeleteRecordsInterceptorsChain
    ): List<DelStatus>
}

class QueryRecordsInterceptorsChain(
    private val resolver: LocalRecordsResolverImpl,
    private val interceptors: Iterator<LocalRecordsInterceptor>
) {
    fun invoke(
        queryArg: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): RecsQueryRes<RecordAtts> {

        return if (interceptors.hasNext()) {
            interceptors.next().queryRecords(queryArg, attributes, rawAtts, this)
        } else {
            resolver.queryImpl(queryArg, attributes, rawAtts)
        }
    }
}

class GetValuesAttsInterceptorsChain(
    private val resolver: LocalRecordsResolverImpl,
    private val interceptors: Iterator<LocalRecordsInterceptor>
) {
    fun invoke(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<RecordAtts> {

        return if (interceptors.hasNext()) {
            interceptors.next().getValuesAtts(values, attributes, rawAtts, this)
        } else {
            resolver.getValueAttsImpl(values, attributes, rawAtts)
        }
    }
}

class GetRecordsAttsInterceptorsChain(
    private val resolver: LocalRecordsResolverImpl,
    private val interceptors: Iterator<LocalRecordsInterceptor>
) {
    fun invoke(
        sourceId: String,
        recordIds: List<String>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<RecordAtts> {

        return if (interceptors.hasNext()) {
            interceptors.next().getRecordsAtts(sourceId, recordIds, attributes, rawAtts, this)
        } else {
            resolver.getRecordsAttsImpl(sourceId, recordIds, attributes, rawAtts)
        }
    }
}

class MutateRecordInterceptorsChain(
    private val resolver: LocalRecordsResolverImpl,
    private val interceptors: Iterator<LocalRecordsInterceptor>
) {
    fun invoke(
        sourceId: String,
        record: LocalRecordAtts,
        attsToLoad: List<SchemaAtt>,
        rawAtts: Boolean,
    ): RecordAtts {

        return if (interceptors.hasNext()) {
            interceptors.next().mutateRecord(sourceId, record, attsToLoad, rawAtts, this)
        } else {
            resolver.mutateRecordImpl(sourceId, record, attsToLoad, rawAtts)
        }
    }
}

class DeleteRecordsInterceptorsChain(
    private val resolver: LocalRecordsResolverImpl,
    private val interceptors: Iterator<LocalRecordsInterceptor>
) {
    fun invoke(
        sourceId: String,
        recordIds: List<String>
    ): List<DelStatus> {

        return if (interceptors.hasNext()) {
            interceptors.next().deleteRecords(sourceId, recordIds, this)
        } else {
            resolver.deleteRecordsImpl(sourceId, recordIds)
        }
    }
}
