package ru.citeck.ecos.records3.record.dao.atts

import ru.citeck.ecos.records3.record.dao.RecordsDao
import kotlin.jvm.Throws

interface RecordsAttsDao : RecordsDao {

    /**
     * Get attributes for records
     * @param recordsId - ids of records
     * @return attributes. Size and order of atts objects must be the same as order of recordsId.
     */
    @Throws(Exception::class)
    fun getRecordsAtts(recordsId: List<String>): List<*>?
}
