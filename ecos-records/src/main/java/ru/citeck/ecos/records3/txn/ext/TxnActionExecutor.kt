package ru.citeck.ecos.records3.txn.ext

interface TxnActionExecutor<T> {

    fun execute(action: T)

    fun getType(): String
}
