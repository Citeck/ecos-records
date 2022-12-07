package ru.citeck.ecos.records3.record.atts.schema.read.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.utils.StringUtils.isBlank
import ru.citeck.ecos.records3.record.atts.proc.AttOrElseProcessor
import ru.citeck.ecos.records3.record.atts.proc.AttProcDef
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.utils.AttStrUtils
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

            val beforeOrElsePart = att.substring(0, orElseDelimIdx)
            var orElsePart = att.substring(orElseDelimIdx + 1, nextDelim)

            if (!AttStrUtils.isInQuotes(orElsePart)) {
                if (orElsePart.isEmpty()) {
                    orElsePart = if (beforeOrElsePart.endsWith("}")) {
                        "{}"
                    } else {
                        val scalarDelimIdx = beforeOrElsePart.indexOf('?')
                        if (scalarDelimIdx > -1) {
                            if (scalarDelimIdx > 2 &&
                                beforeOrElsePart[scalarDelimIdx - 1] == ']' &&
                                beforeOrElsePart[scalarDelimIdx - 2] == '['
                            ) {
                                "[]"
                            } else {
                                when (beforeOrElsePart.substring(scalarDelimIdx)) {
                                    ScalarType.JSON_SCHEMA -> "{}"
                                    ScalarType.BOOL_SCHEMA -> "false"
                                    ScalarType.NUM_SCHEMA -> "0"
                                    else -> "''"
                                }
                            }
                        } else {
                            "''"
                        }
                    }
                } else if (orElsePart != "null" &&
                    orElsePart != "true" &&
                    orElsePart != "false" &&
                    orElsePart[0] != '[' &&
                    orElsePart[0] != '{' &&
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

            att = "$beforeOrElsePart|or($orElsePart)" + if (att.length > nextDelim) {
                att.substring(nextDelim)
            } else {
                ""
            }

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
                    it[0] == '[' || it[0] == '{' -> DataValue.create(it)
                    Character.isDigit(it[0]) -> DataValue.create(it.toDouble())
                    else -> DataValue.createStr(AttStrUtils.removeQuotes(it))
                }
            }
            .forEach { e: DataValue -> result.add(e) }
        return result
    }
}
