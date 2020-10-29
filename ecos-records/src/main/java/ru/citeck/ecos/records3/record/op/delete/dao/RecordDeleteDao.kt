package ru.citeck.ecos.records3.record.op.delete.dao

import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus

interface RecordDeleteDao : RecordsDao {

    fun delete(recordId: String): DelStatus
}
