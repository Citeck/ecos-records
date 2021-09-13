package ru.citeck.ecos.records3.txn

/**
 * Local transactions service
 */
interface RecordsTxnService {

    fun <T> doInTransaction(readOnly: Boolean, action: () -> T): T
}
