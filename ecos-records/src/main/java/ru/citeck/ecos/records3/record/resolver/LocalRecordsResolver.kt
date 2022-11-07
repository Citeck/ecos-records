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

    fun query(
        queryArg: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): RecsQueryRes<RecordAtts>

    fun getValueAtts(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<RecordAtts>

    fun getRecordAtts(
        sourceId: String,
        recordsId: List<String>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<RecordAtts>

    fun mutateRecords(
        sourceId: String,
        records: List<LocalRecordAtts>,
        attsToLoad: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<RecordAtts>

    fun deleteRecords(
        sourceId: String,
        recordsId: List<String>
    ): List<DelStatus>

    fun isSourceTransactional(sourceId: String): Boolean

    fun commit(sourceId: String, recordsId: List<String>)

    fun rollback(sourceId: String, recordsId: List<String>)

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
