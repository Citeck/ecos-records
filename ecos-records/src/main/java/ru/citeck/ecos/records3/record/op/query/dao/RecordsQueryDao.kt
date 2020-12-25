package ru.citeck.ecos.records3.record.op.query.dao

import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery

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

    fun queryRecords(query: RecordsQuery): Any?
}
