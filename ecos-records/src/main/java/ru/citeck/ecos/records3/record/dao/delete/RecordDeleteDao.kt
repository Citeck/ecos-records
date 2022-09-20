package ru.citeck.ecos.records3.record.dao.delete

import ru.citeck.ecos.records3.record.dao.RecordsDao
import kotlin.jvm.Throws

interface RecordDeleteDao : RecordsDao {

    @Throws(Exception::class)
    fun delete(recordId: String): DelStatus
}
