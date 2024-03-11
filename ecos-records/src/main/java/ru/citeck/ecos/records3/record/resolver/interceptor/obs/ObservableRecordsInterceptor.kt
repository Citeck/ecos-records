package ru.citeck.ecos.records3.record.resolver.interceptor.obs

import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.resolver.interceptor.*

open class ObservableRecordsInterceptor(
    private val services: RecordsServiceFactory
) : LocalRecordsInterceptor {

    private val context = services.micrometerContext

    override fun queryRecords(
        queryArg: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: QueryRecordsInterceptorsChain
    ): RecsQueryRes<RecordAtts> {

        return context.createObs(
            LocalRecordsObsCtx.Query(
                queryArg,
                attributes,
                rawAtts,
                services
            )
        ).observe { chain.invoke(queryArg, attributes, rawAtts) }
    }

    override fun getRecordsAtts(
        sourceId: String,
        recordIds: List<String>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: GetRecordsAttsInterceptorsChain
    ): List<RecordAtts> {

        return context.createObs(
            LocalRecordsObsCtx.GetAtts(
                sourceId,
                recordIds,
                attributes,
                rawAtts,
                services
            )
        ).observe { chain.invoke(sourceId, recordIds, attributes, rawAtts) }
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

        return context.createObs(
            LocalRecordsObsCtx.Mutate(
                sourceId,
                record,
                attsToLoad,
                rawAtts,
                services
            )
        ).observe { chain.invoke(sourceId, record, attsToLoad, rawAtts) }
    }

    override fun deleteRecords(
        sourceId: String,
        recordIds: List<String>,
        chain: DeleteRecordsInterceptorsChain
    ): List<DelStatus> {

        return context.createObs(
            LocalRecordsObsCtx.Delete(
                sourceId,
                recordIds,
                services
            )
        ).observe { chain.invoke(sourceId, recordIds) }
    }
}
