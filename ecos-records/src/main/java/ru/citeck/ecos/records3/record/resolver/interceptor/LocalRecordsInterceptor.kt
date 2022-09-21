package ru.citeck.ecos.records3.record.resolver.interceptor

import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.resolver.LocalRecordsResolverImpl
import ru.citeck.ecos.webapp.api.entity.EntityRef

interface LocalRecordsInterceptor {

    fun query(
        queryArg: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: QueryInterceptorsChain
    ): RecsQueryRes<RecordAtts>

    fun getAtts(
        records: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: GetAttsInterceptorsChain
    ): List<RecordAtts>

    fun mutate(
        records: List<RecordAtts>,
        attsToLoad: List<List<SchemaAtt>>,
        rawAtts: Boolean,
        chain: MutateInterceptorsChain
    ): List<RecordAtts>

    fun delete(
        records: List<EntityRef>,
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

class GetAttsInterceptorsChain(
    private val resolver: LocalRecordsResolverImpl,
    private val interceptors: Iterator<LocalRecordsInterceptor>
) {
    fun invoke(
        records: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<RecordAtts> {

        return if (interceptors.hasNext()) {
            interceptors.next().getAtts(records, attributes, rawAtts, this)
        } else {
            resolver.getAttsImpl(records, attributes, rawAtts)
        }
    }
}

class MutateInterceptorsChain(
    private val resolver: LocalRecordsResolverImpl,
    private val interceptors: Iterator<LocalRecordsInterceptor>
) {
    fun invoke(
        records: List<RecordAtts>,
        attsToLoad: List<List<SchemaAtt>>,
        rawAtts: Boolean,
    ): List<RecordAtts> {

        return if (interceptors.hasNext()) {
            interceptors.next().mutate(records, attsToLoad, rawAtts, this)
        } else {
            resolver.mutateImpl(records, attsToLoad, rawAtts)
        }
    }
}

class DeleteInterceptorsChain(
    private val resolver: LocalRecordsResolverImpl,
    private val interceptors: Iterator<LocalRecordsInterceptor>
) {
    fun invoke(
        records: List<EntityRef>
    ): List<DelStatus> {

        return if (interceptors.hasNext()) {
            interceptors.next().delete(records, this)
        } else {
            resolver.deleteImpl(records)
        }
    }
}
