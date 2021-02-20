package ru.citeck.ecos.records3.record.dao.atts

import ru.citeck.ecos.records3.record.dao.RecordsDao

interface RecordsAttsDao : RecordsDao {

    fun getRecordsAtts(recordsId: List<String>): List<*>?
}
