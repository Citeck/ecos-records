package ru.citeck.ecos.records3.record.dao.atts

import ru.citeck.ecos.records3.record.dao.RecordsDao
import kotlin.jvm.Throws

interface RecordsAttsDao : RecordsDao {

    @Throws(Exception::class)
    fun getRecordsAtts(recordsId: List<String>): List<*>?
}
