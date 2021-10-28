package ru.citeck.ecos.records3.record.atts.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import java.util.*

class AttOrElseProcessor : AttProcessor {

    companion object {
        const val ATT_PREFIX: String = "a:"
    }

    override fun process(attributes: ObjectData, value: DataValue, args: List<DataValue>): Any? {

        if (args.isEmpty()) {
            return value
        }
        if (value.isNotNull()) {
            if (!value.isTextual() || value.asText().isNotEmpty()) {
                return value
            }
        }

        var newValue = value
        for (orElseAtt in args) {
            newValue = if (orElseAtt.isTextual()) {
                val txtAtt: String = orElseAtt.asText()
                if (txtAtt.startsWith(ATT_PREFIX)) {
                    attributes.get(txtAtt.substring(ATT_PREFIX.length))
                } else {
                    orElseAtt
                }
            } else {
                orElseAtt
            }
            if (value.isNotNull() && (!value.isTextual() || value.asText().isNotEmpty())) {
                return value
            }
        }
        return newValue
    }

    override fun getAttsToLoad(arguments: List<DataValue>): Collection<String> {
        val attsToLoad: MutableSet<String> = HashSet()
        for (orElseAtt in arguments) {
            if (!orElseAtt.isTextual()) {
                continue
            }
            val txtAtt: String = orElseAtt.asText()
            if (txtAtt.startsWith(ATT_PREFIX) && txtAtt.length > ATT_PREFIX.length) {
                attsToLoad.add(txtAtt.substring(ATT_PREFIX.length))
            }
        }
        return attsToLoad
    }

    override fun getType(): String = "or"
}
