package ru.citeck.ecos.records3.record.resolver

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.RecordsDaoInfo
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import java.util.concurrent.ScheduledExecutorService

interface LocalRecordsResolver {

    fun initJobs(executor: ScheduledExecutorService?)

    fun query(queryArg: RecordsQuery, attributes: List<SchemaAtt>, rawAtts: Boolean): RecsQueryRes<RecordAtts>

    fun getAtts(records: List<*>, attributes: List<SchemaAtt>, rawAtts: Boolean): List<RecordAtts>

    fun mutate(records: List<RecordAtts>): List<RecordRef>

    fun delete(records: List<RecordRef>): List<DelStatus>

    fun register(sourceId: String, recordsDao: RecordsDao)

    fun getSourceInfo(sourceId: String): RecordsDaoInfo?

    fun getSourceInfo() : List<RecordsDaoInfo>

    fun containsDao(id: String): Boolean
}
