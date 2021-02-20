package ru.citeck.ecos.records3.record.dao.delete

import ru.citeck.ecos.records3.record.dao.RecordsDao

interface RecordsDeleteDao : RecordsDao {

    fun delete(recordsId: List<String>): List<DelStatus>
}
