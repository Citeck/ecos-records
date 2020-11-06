package ru.citeck.ecos.records3.record.op.query.dao

import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery

interface RecordsQueryDao : RecordsDao {

    fun queryRecords(query: RecordsQuery): RecsQueryRes<*>?
}
