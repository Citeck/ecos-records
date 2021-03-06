package ru.citeck.ecos.records3.record.atts.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData

class AttPrefixSuffixProcessor : AbstractAttProcessor<AttPrefixSuffixProcessor.Args>() {

    override fun processOne(attributes: ObjectData, value: DataValue, args: Args): Any? {
        val text = value.asText()
        return if (text.isEmpty()) {
            value
        } else {
            args.prefix + value.asText() + args.suffix
        }
    }

    override fun parseArgs(args: List<DataValue>): Args {
        var prefix = ""
        var suffix = ""
        if (args.isNotEmpty()) {
            prefix = args[0].asText()
        }
        if (args.size > 1) {
            suffix = args[1].asText()
        }
        return Args(prefix, suffix)
    }

    override fun getType(): String = "presuf"

    class Args(val prefix: String, val suffix: String)
}
