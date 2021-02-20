package ru.citeck.ecos.records3.record.dao.query

import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery

/**
 * You can return:
 *
 * List<*>
 * Set<*>
 * RecsQueryRes<*>
 * DataValue with array
 *
 * if none of any types above returned, then listOf(result) will be returned from DAO
 */
interface RecordsQueryDao : RecordsDao {

    fun queryRecords(recsQuery: RecordsQuery): Any?
}
