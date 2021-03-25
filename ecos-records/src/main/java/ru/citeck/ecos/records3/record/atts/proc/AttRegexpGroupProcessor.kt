package ru.citeck.ecos.records3.record.atts.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData

class AttRegexpGroupProcessor : AbstractAttProcessor<AttRegexpGroupProcessor.Arguments>(false) {

    override fun processOne(attributes: ObjectData, value: DataValue, args: Arguments): Any? {

        if (!value.isTextual() || args.pattern == null) {
            return null
        }

        val matcher = args.pattern.matchEntire(value.asText()) ?: return null
        val values = matcher.groupValues

        if (args.groupIdx >= values.size) {
            return null
        }

        return values[args.groupIdx]
    }

    override fun parseArgs(args: List<DataValue>): Arguments {

        if (args.isEmpty()) {
            return Arguments(null, 1)
        }

        val pattern = args[0].asText().toRegex()
        if (args.size == 1) {
            return Arguments(pattern, 1)
        }

        return Arguments(pattern, args[1].asInt(1))
    }

    override fun getType(): String = "rxg"

    class Arguments(
        val pattern: Regex?,
        val groupIdx: Int
    )
}
