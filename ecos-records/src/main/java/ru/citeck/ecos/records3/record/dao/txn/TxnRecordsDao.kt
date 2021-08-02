package ru.citeck.ecos.records3.record.dao.txn

import ru.citeck.ecos.records3.record.dao.RecordsDao
import java.util.*

interface TxnRecordsDao : RecordsDao {

    fun commit(txnId: UUID, recordsId: List<String>)

    fun rollback(txnId: UUID, recordsId: List<String>)
}
