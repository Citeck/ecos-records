package ru.citeck.ecos.records3.record.op.atts.service.proc

import ru.citeck.ecos.commons.data.DataValue

data class AttProcDef(
    val type: String,
    val arguments: List<DataValue>
)
