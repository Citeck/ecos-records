package ru.citeck.ecos.records3.record.op.atts.service.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import java.util.function.Consumer

class AttJoinProcessor : AttProcessor {

    override fun process(
        attributes: ObjectData,
        value: DataValue,
        args: List<DataValue>
    ): Any {

        if (!value.isArray()) {
            return value
        }
        if (value.size() == 0) {
            return ""
        }
        val delim = if (args.isNotEmpty()) args[0].asText() else ","
        val sb = StringBuilder()
        value.forEach(Consumer { v: DataValue -> sb.append(v.asText()).append(delim) })
        return DataValue.createStr(sb.substring(0, sb.length - delim.length))
    }

    override fun getType() = "join"
}
