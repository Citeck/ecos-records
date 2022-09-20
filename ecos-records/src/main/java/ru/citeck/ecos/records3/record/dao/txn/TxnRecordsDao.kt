package ru.citeck.ecos.records3.record.dao.txn

import ru.citeck.ecos.records3.record.dao.RecordsDao
import java.util.*
import kotlin.jvm.Throws

interface TxnRecordsDao : RecordsDao {

    @Throws(Exception::class)
    fun commit(txnId: UUID, recordsId: List<String>)

    @Throws(Exception::class)
    fun rollback(txnId: UUID, recordsId: List<String>)

    fun isTransactional(): Boolean = true
}
