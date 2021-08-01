package ru.citeck.ecos.records3.record.resolver

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.impl.source.RecordsSourceMeta
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes

interface LocalRecordsResolver {

    fun query(queryArg: RecordsQuery, attributes: List<SchemaAtt>, rawAtts: Boolean): RecsQueryRes<RecordAtts>

    fun getAtts(records: List<*>, attributes: List<SchemaAtt>, rawAtts: Boolean): List<RecordAtts>

    fun mutate(records: List<RecordAtts>): List<RecordRef>

    fun delete(records: List<RecordRef>): List<DelStatus>

    fun commit(recordRefs: List<RecordRef>)

    fun rollback(recordRefs: List<RecordRef>)

    fun register(sourceId: String, recordsDao: RecordsDao)

    fun unregister(sourceId: String)

    fun getSourceInfo(sourceId: String): RecordsSourceMeta?

    fun getSourcesInfo(): List<RecordsSourceMeta>

    fun containsDao(id: String): Boolean

    fun <T : Any> getRecordsDao(sourceId: String, type: Class<T>): T?
}
