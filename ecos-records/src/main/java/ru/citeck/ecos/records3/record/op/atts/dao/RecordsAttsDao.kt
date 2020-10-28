package ru.citeck.ecos.records3.record.op.atts.dao

import ru.citeck.ecos.records3.record.dao.RecordsDao

interface RecordsAttsDao : RecordsDao {

    fun getRecordsAtts(records: List<String>): List<*>?
}
