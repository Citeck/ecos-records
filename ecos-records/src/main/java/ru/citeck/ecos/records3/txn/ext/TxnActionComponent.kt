package ru.citeck.ecos.records3.txn.ext

interface TxnActionComponent<T> {

    /**
     * Merge or update actions list.
     * This method executed before query result will
     * be returned and after result was received.
     *
     * fromRemote is true when we call preProcess with actions which was received from remote query.
     */
    fun preProcess(actions: List<T>, fromRemote: Boolean): List<T>

    fun execute(action: T)

    fun getType(): String
}
