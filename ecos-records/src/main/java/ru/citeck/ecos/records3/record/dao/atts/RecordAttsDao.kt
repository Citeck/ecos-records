package ru.citeck.ecos.records3.record.dao.atts

import ru.citeck.ecos.records3.record.dao.RecordsDao
import kotlin.jvm.Throws

interface RecordAttsDao : RecordsDao {

    /**
     * It is adapter to RecordsAttsDao for single record
     * @see RecordsAttsDao
     */
    @Throws(Exception::class)
    fun getRecordAtts(recordId: String): Any?
}
