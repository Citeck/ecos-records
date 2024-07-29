package ru.citeck.ecos.records3.record.resolver

import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.impl.source.RecordsSourceMeta
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.resolver.interceptor.LocalRecordsInterceptor

interface LocalRecordsResolver {

    fun queryRecords(
        queryArg: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): RecsQueryRes<RecordAtts>

    fun getValuesAtts(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<RecordAtts>

    fun getRecordsAtts(
        sourceId: String,
        recordIds: List<String>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<RecordAtts>

    fun mutateRecord(
        sourceId: String,
        record: LocalRecordAtts,
        attsToLoad: List<SchemaAtt>,
        rawAtts: Boolean
    ): RecordAtts

    fun deleteRecords(
        sourceId: String,
        recordIds: List<String>
    ): List<DelStatus>

    fun register(sourceId: String, recordsDao: RecordsDao)

    fun unregister(sourceId: String)

    fun getSourceInfo(sourceId: String): RecordsSourceMeta?

    fun getSourcesInfo(): List<RecordsSourceMeta>

    fun containsDao(id: String): Boolean

    fun <T : Any> getRecordsDao(sourceId: String, type: Class<T>): T?

    fun hasDaoWithEmptyId(): Boolean

    fun getInterceptors(): List<LocalRecordsInterceptor>

    fun addInterceptors(interceptors: List<LocalRecordsInterceptor>)

    fun addInterceptor(interceptor: LocalRecordsInterceptor)

    fun setInterceptors(interceptors: List<LocalRecordsInterceptor>)
}
