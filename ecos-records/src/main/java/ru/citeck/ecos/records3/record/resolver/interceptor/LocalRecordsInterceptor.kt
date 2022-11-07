package ru.citeck.ecos.records3.record.resolver.interceptor

import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.resolver.LocalRecordsResolverImpl

interface LocalRecordsInterceptor {

    fun query(
        queryArg: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: QueryInterceptorsChain
    ): RecsQueryRes<RecordAtts>

    fun getValueAtts(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: GetValueAttsInterceptorsChain
    ): List<RecordAtts>

    fun getRecordAtts(
        sourceId: String,
        recordsId: List<String>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: GetRecordAttsInterceptorsChain
    ): List<RecordAtts>

    fun mutateRecords(
        sourceId: String,
        records: List<LocalRecordAtts>,
        attsToLoad: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: MutateRecordsInterceptorsChain
    ): List<RecordAtts>

    fun deleteRecords(
        sourceId: String,
        recordsId: List<String>,
        chain: DeleteInterceptorsChain
    ): List<DelStatus>
}

class QueryInterceptorsChain(
    private val resolver: LocalRecordsResolverImpl,
    private val interceptors: Iterator<LocalRecordsInterceptor>
) {
    fun invoke(
        queryArg: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): RecsQueryRes<RecordAtts> {

        return if (interceptors.hasNext()) {
            interceptors.next().query(queryArg, attributes, rawAtts, this)
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
        recordsId: List<String>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<RecordAtts> {

        return if (interceptors.hasNext()) {
            interceptors.next().getRecordAtts(sourceId, recordsId, attributes, rawAtts, this)
        } else {
            resolver.getRecordAttsImpl(sourceId, recordsId, attributes, rawAtts)
        }
    }
}

class MutateRecordsInterceptorsChain(
    private val resolver: LocalRecordsResolverImpl,
    private val interceptors: Iterator<LocalRecordsInterceptor>
) {
    fun invoke(
        sourceId: String,
        records: List<LocalRecordAtts>,
        attsToLoad: List<SchemaAtt>,
        rawAtts: Boolean,
    ): List<RecordAtts> {

        return if (interceptors.hasNext()) {
            interceptors.next().mutateRecords(sourceId, records, attsToLoad, rawAtts, this)
        } else {
            resolver.mutateRecordsImpl(sourceId, records, attsToLoad, rawAtts)
        }
    }
}

class DeleteInterceptorsChain(
    private val resolver: LocalRecordsResolverImpl,
    private val interceptors: Iterator<LocalRecordsInterceptor>
) {
    fun invoke(
        sourceId: String,
        recordsId: List<String>
    ): List<DelStatus> {

        return if (interceptors.hasNext()) {
            interceptors.next().deleteRecords(sourceId, recordsId, this)
        } else {
            resolver.deleteRecordsImpl(sourceId, recordsId)
        }
    }
}
