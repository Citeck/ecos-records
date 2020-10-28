package ru.citeck.ecos.records3.record.op.atts.service.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData

class AttPrefixSuffixProcessor : AbstractAttProcessor<AttPrefixSuffixProcessor.Args>() {

    override fun processOne(attributes: ObjectData, value: DataValue, args: Args): Any? {
        return args.prefix + value.asText() + args.suffix
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
