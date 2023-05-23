package ru.citeck.ecos.records3.record.atts.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData

class AttLowerCaseProcessor : AttProcessor {

    companion object {
        const val TYPE = "lowercase"
    }

    override fun process(attributes: ObjectData, value: DataValue, args: List<DataValue>): Any? {
        if (value.isTextual()) {
            return value.asText().lowercase()
        }
        return value
    }

    override fun getType(): String {
        return TYPE
    }
}
