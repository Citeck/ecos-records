package ru.citeck.ecos.records3.record.op.atts.service.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData

interface AttProcessor {

    fun process(attributes: ObjectData, value: DataValue, args: List<DataValue>): Any?

    fun getAttsToLoad(arguments: List<DataValue>): Collection<String> = emptySet()

    fun getType(): String
}
