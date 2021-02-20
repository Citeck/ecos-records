package ru.citeck.ecos.records3.record.dao.atts

import ru.citeck.ecos.records3.record.dao.RecordsDao

interface RecordAttsDao : RecordsDao {

    fun getRecordAtts(recordId: String): Any?
}
