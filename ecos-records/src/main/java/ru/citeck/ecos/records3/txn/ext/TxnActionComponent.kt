package ru.citeck.ecos.records3.txn.ext

interface TxnActionComponent<T> {

    fun execute(actions: List<T>)

    fun getType(): String
}
