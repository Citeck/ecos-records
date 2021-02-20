package ru.citeck.ecos.records3.record.dao.delete

import ru.citeck.ecos.records3.record.dao.RecordsDao

interface RecordDeleteDao : RecordsDao {

    fun delete(recordId: String): DelStatus
}
