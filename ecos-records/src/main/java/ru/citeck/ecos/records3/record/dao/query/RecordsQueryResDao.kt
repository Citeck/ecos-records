package ru.citeck.ecos.records3.record.dao.query

import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes

interface RecordsQueryResDao : RecordsDao {

    fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>?
}
