package ru.citeck.ecos.records3.record.op.query.dao

import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery

interface RecordsQueryDao : RecordsDao {

    fun queryRecords(query: RecordsQuery): RecsQueryRes<*>?

    /**
     * Get query languages which can be used to query records in this DAO.
     * First languages in the result list are more preferred than last
     *
     * @return list of languages
     */
    fun getSupportedLanguages(): List<String> = emptyList()

    fun isGroupingSupported() : Boolean = false
}
