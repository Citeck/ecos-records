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

    fun getValueAtts(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: GetValueAttsInterceptorsChain
    ): List<RecordAtts>

    fun getRecordAtts(
        sourceId: String,
        recordIds: List<String>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: GetRecordAttsInterceptorsChain
    ): List<RecordAtts>

    fun mutateRecord(
        sourceId: String,
        record: LocalRecordAtts,
        attsToLoad: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: MutateRecordsInterceptorsChain
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

class GetValueAttsInterceptorsChain(
    private val resolver: LocalRecordsResolverImpl,
    private val interceptors: Iterator<LocalRecordsInterceptor>
) {
    fun invoke(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<RecordAtts> {

        return if (interceptors.hasNext()) {
            interceptors.next().getValueAtts(values, attributes, rawAtts, this)
        } else {
            resolver.getValueAttsImpl(values, attributes, rawAtts)
        }
    }
}

class GetRecordAttsInterceptorsChain(
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
            interceptors.next().getRecordAtts(sourceId, recordIds, attributes, rawAtts, this)
        } else {
            resolver.getRecordAttsImpl(sourceId, recordIds, attributes, rawAtts)
        }
    }
}

class MutateRecordsInterceptorsChain(
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
