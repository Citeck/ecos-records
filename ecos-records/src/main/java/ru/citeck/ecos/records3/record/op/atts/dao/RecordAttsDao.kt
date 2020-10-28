package ru.citeck.ecos.records3.record.op.atts.dao

import ru.citeck.ecos.records3.record.dao.RecordsDao

interface RecordAttsDao : RecordsDao {

    fun getRecordAtts(record: String): Any?
}
