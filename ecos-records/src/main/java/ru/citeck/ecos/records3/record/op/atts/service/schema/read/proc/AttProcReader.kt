package ru.citeck.ecos.records3.record.op.atts.service.schema.read.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.utils.StringUtils.isBlank
import ru.citeck.ecos.records2.meta.util.AttStrUtils
import ru.citeck.ecos.records3.record.op.atts.service.proc.AttOrElseProcessor
import ru.citeck.ecos.records3.record.op.atts.service.proc.AttProcDef
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.min

class AttProcReader {

    companion object {
        private val PROCESSOR_PATTERN = Pattern.compile("(.+?)\\((.*)\\)")
    }

    fun read(attribute: String): AttWithProc {

        val processors: List<AttProcDef>
        var att = attribute
        var orElseDelimIdx = AttStrUtils.indexOf(att, '!')

        while (orElseDelimIdx > 0) {

            val nextDelim0 = AttStrUtils.indexOf(att, "|", orElseDelimIdx + 1)
            val nextDelim1 = AttStrUtils.indexOf(att, "!", orElseDelimIdx + 1)
            var nextDelim = when {
                nextDelim0 == -1 -> nextDelim1
                nextDelim1 == -1 -> nextDelim0
                else -> min(nextDelim0, nextDelim1)
            }
            if (nextDelim == -1) {
                nextDelim = att.length
            }

            var orElsePart = att.substring(orElseDelimIdx + 1, nextDelim)

            if (!AttStrUtils.isInQuotes(orElsePart)) {
                if (orElsePart.isEmpty()) {
                    orElsePart = "''"
                } else if (orElsePart != "null" &&
                    orElsePart != "true" &&
                    orElsePart != "false" &&
                    !Character.isDigit(orElsePart[0])
                ) {
                    orElsePart = (
                        "'" +
                            AttOrElseProcessor.ATT_PREFIX +
                            orElsePart.replace("'", "\\'") +
                            "'"
                        )
                }
            }

            att = (
                att.substring(0, orElseDelimIdx) + "|or(" + orElsePart + ")" +
                    if (att.length > nextDelim) att.substring(nextDelim) else ""
                )

            orElseDelimIdx = AttStrUtils.indexOf(att, '!')
        }
        val pipeDelimIdx = AttStrUtils.indexOf(att, '|')
        if (pipeDelimIdx > 0) {
            processors = parseProcessors(att.substring(pipeDelimIdx + 1))
            att = att.substring(0, pipeDelimIdx)
        } else {
            processors = emptyList()
        }
        return AttWithProc(att, processors)
    }

    private fun parseProcessors(processorStr: String): List<AttProcDef> {
        if (isBlank(processorStr)) {
            return emptyList()
        }
        val result: MutableList<AttProcDef> = ArrayList()
        for (proc in AttStrUtils.split(processorStr, '|')) {
            val matcher: Matcher = PROCESSOR_PATTERN.matcher(proc)
            if (matcher.matches()) {
                val type = matcher.group(1).trim { it <= ' ' }
                val args = parseArgs(matcher.group(2).trim { it <= ' ' })
                result.add(AttProcDef(type, args))
            }
        }
        return result
    }

    private fun parseArgs(argsStr: String): List<DataValue> {
        val result = ArrayList<DataValue>()
        if (isBlank(argsStr)) {
            return result
        }
        AttStrUtils.split(argsStr, ",")
            .map { it.trim() }
            .map {
                when {
                    it.isEmpty() -> DataValue.createStr("")
                    it == "null" -> DataValue.NULL
                    it == "true" -> DataValue.TRUE
                    it == "false" -> DataValue.FALSE
                    Character.isDigit(it[0]) -> DataValue.create(it.toDouble())
                    else -> {
                        val withoutQuotes = AttStrUtils.removeQuotes(it)
                        DataValue.createStr(AttStrUtils.removeEscaping(withoutQuotes))
                    }
                }
            }
            .forEach { e: DataValue -> result.add(e) }
        return result
    }
}
