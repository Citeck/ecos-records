package ru.citeck.ecos.records3.record.atts.schema.read.proc

import com.github.benmanes.caffeine.cache.Caffeine
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.utils.StringUtils.isBlank
import ru.citeck.ecos.records3.record.atts.proc.AttOrElseProcessor
import ru.citeck.ecos.records3.record.atts.proc.AttProcDef
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.read.ParseUtils
import ru.citeck.ecos.records3.record.atts.schema.utils.AttStrUtils
import kotlin.math.min

class AttProcReader {

    companion object {
        private const val CACHE_MAX_SIZE = 1000L
        private const val CACHE_MAX_KEY_LENGTH = 256
    }

    private val cache = Caffeine.newBuilder()
        .maximumSize(CACHE_MAX_SIZE)
        .build<String, AttWithProc>()

    fun read(attribute: String): AttWithProc {
        if (attribute.length > CACHE_MAX_KEY_LENGTH) {
            return readInternal(attribute)
        }
        return cache.get(attribute) { readInternal(it) }
    }

    private fun readInternal(attribute: String): AttWithProc {
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
                        if (AttStrUtils.indexOf(beforeOrElsePart, "[]") != -1) {
                            "[]"
                        } else {
                            "{}"
                        }
                    } else if (beforeOrElsePart.contains("[]")) {
                        "[]"
                    } else {
                        val scalarDelimIdx = beforeOrElsePart.indexOf('?')
                        if (scalarDelimIdx > -1) {
                            when (beforeOrElsePart.substring(scalarDelimIdx)) {
                                ScalarType.JSON_SCHEMA -> "{}"
                                ScalarType.BOOL_SCHEMA -> "false"
                                ScalarType.NUM_SCHEMA -> "0.0"
                                else -> "''"
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
        val result = ArrayList<AttProcDef>()
        for (proc in AttStrUtils.split(processorStr, '|')) {
            val trimmedProc = proc.trim()
            val parsed = parseProcessor(trimmedProc)
            if (parsed != null) {
                result.add(parsed)
            }
        }
        return result
    }

    /**
     * Parses processor like "name(arg1, arg2)" without using regex
     */
    private fun parseProcessor(proc: String): AttProcDef? {
        val openParen = proc.indexOf('(')
        if (openParen == -1) return null

        val closeParen = proc.lastIndexOf(')')
        if (closeParen == -1 || closeParen < openParen) return null

        val type = proc.substring(0, openParen).trim()
        if (type.isEmpty()) return null

        val argsStr = proc.substring(openParen + 1, closeParen).trim()
        val args = parseArgs(argsStr)

        return AttProcDef(type, args)
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
                    ParseUtils.isNumValue(it) -> ParseUtils.parseNumValue(it)
                    else -> DataValue.createStr(AttStrUtils.removeQuotes(it))
                }
            }
            .forEach { e: DataValue -> result.add(e) }
        return result
    }
}
