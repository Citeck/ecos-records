package ru.citeck.ecos.records3.record.op.delete.dao

import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus

interface RecordsDeleteDao : RecordsDao {

    fun delete(records: List<String>): List<DelStatus>
}
