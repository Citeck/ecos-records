package ru.citeck.ecos.records3.txn

class DefaultRecordsTxnService : RecordsTxnService {

    override fun <T> doInTransaction(readOnly: Boolean, action: () -> T): T {
        return action.invoke()
    }
}
