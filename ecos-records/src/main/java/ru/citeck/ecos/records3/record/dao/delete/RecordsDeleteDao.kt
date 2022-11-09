package ru.citeck.ecos.records3.record.dao.delete

import ru.citeck.ecos.records3.record.dao.RecordsDao
import kotlin.jvm.Throws

interface RecordsDeleteDao : RecordsDao {

    @Throws(Exception::class)
    fun delete(recordIds: List<String>): List<DelStatus>
}
