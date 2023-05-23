package ru.citeck.ecos.records3.record.atts.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData

class AttUpperCaseProcessor : AttProcessor {

    companion object {
        const val TYPE = "uppercase"
    }

    override fun process(attributes: ObjectData, value: DataValue, args: List<DataValue>): Any? {
        if (value.isTextual()) {
            return value.asText().uppercase()
        }
        return value
    }

    override fun getType(): String {
        return TYPE
    }
}
