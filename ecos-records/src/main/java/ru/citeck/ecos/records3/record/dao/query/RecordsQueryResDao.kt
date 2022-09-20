package ru.citeck.ecos.records3.record.dao.query

import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import kotlin.jvm.Throws

interface RecordsQueryResDao : RecordsDao {

    @Throws(Exception::class)
    fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>?
}
