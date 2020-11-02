package ru.citeck.ecos.records3.txn

interface RecordsTxnService {

    fun <T> doInTransaction(readOnly: Boolean, action: () -> T): T
}
