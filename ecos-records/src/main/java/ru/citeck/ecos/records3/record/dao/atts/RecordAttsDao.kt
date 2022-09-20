package ru.citeck.ecos.records3.record.dao.atts

import ru.citeck.ecos.records3.record.dao.RecordsDao
import kotlin.jvm.Throws

interface RecordAttsDao : RecordsDao {

    @Throws(Exception::class)
    fun getRecordAtts(recordId: String): Any?
}
