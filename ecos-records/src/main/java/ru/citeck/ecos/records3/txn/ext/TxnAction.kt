package ru.citeck.ecos.records3.txn.ext

import ru.citeck.ecos.commons.data.DataValue

data class TxnAction(
    val type: String,
    val data: DataValue
)
