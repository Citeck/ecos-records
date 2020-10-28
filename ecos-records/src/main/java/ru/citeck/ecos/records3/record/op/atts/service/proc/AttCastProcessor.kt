package ru.citeck.ecos.records3.record.op.atts.service.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData

class AttCastProcessor : AbstractAttProcessor<List<DataValue>>() {

    override fun processOne(attributes: ObjectData, value: DataValue, args: List<DataValue>): Any? {

        if (args.isEmpty()) {
            return value
        }
        when (args[0].asText()) {
            "str" -> return value.asText()
            "num" -> return value.asDouble()
            "bool" -> return value.asBoolean()
        }
        return null
    }

    override fun parseArgs(args: List<DataValue>): List<DataValue> {
        return args
    }

    override fun getType(): String = "cast"
}
