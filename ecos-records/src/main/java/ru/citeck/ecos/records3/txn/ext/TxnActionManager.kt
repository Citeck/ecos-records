package ru.citeck.ecos.records3.txn.ext

interface TxnActionManager {

    fun execute(action: TxnAction)

    fun execute(entities: List<TxnAction>)

    fun execute(type: String, data: Any)

    fun executeRaw(actions: List<RawTxnAction>)

    fun register(executor: TxnActionExecutor<*>)

    fun getTxnActions(): List<TxnAction>
}
