package ru.citeck.ecos.records3.record.atts.schema.write

import ru.citeck.ecos.records2.meta.util.AttStrUtils
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt

class AttSchemaWriterV2 : AttSchemaWriter {

    companion object {
        @JvmField
        val INSTANCE = AttSchemaWriterV2()
    }

    fun write(attribute: SchemaAtt, out: StringBuilder) {
        write(attribute, out, false)
    }

    override fun write(attribute: SchemaAtt, out: StringBuilder, firstInBraces: Boolean) {

        if (firstInBraces && attribute.alias.isNotEmpty() && attribute.alias != attribute.name) {
            writeAndEscape(attribute.alias, out, ":,")
            out.append(':')
            writeAndEscape(attribute.name, out, ",.")
        } else {
            writeAndEscape(attribute.name, out, if (firstInBraces) { ":,." } else { ",." })
        }
        if (attribute.multiple) {
            out.append("[]")
        }

        if (attribute.inner.isNotEmpty()) {
            if (attribute.inner.size == 1 &&
                attribute.inner[0].let { it.processors.isEmpty() && it.alias.isEmpty() }
            ) {
                val innerAtt = attribute.inner[0]
                if (!innerAtt.isScalar()) {
                    out.append('.')
                    write(attribute.inner[0], out, false)
                } else {
                    if (innerAtt.name != ScalarType.DISP.schema) {
                        out.append(innerAtt.name)
                    }
                }
            } else {
                out.append('{')
                attribute.inner.forEach {
                    write(it, out, true)
                    out.append(',')
                }
                out.setLength(out.length - 1)
                out.append('}')
            }
        }

        for (processor in attribute.processors) {
            out.append("|").append(processor.type).append("(")
            val argsSize = processor.arguments.size
            for (i in 0 until argsSize) {
                val arg = processor.arguments[i]
                if (arg.isTextual()) {
                    val rawStr = arg.toString()
                    out.append('\'')
                    for (idx in 1 until rawStr.lastIndex) {
                        val ch = rawStr[idx]
                        if (ch == '\\' && idx < rawStr.lastIndex && rawStr[idx + 1] == '"') {
                            // skip escaping
                        } else if (ch == '\'') {
                            out.append('\\').append(ch)
                        } else {
                            out.append(ch)
                        }
                    }
                    out.append('\'')
                } else {
                    out.append(arg.toString())
                }
                if (i < argsSize - 1) {
                    out.append(',')
                }
            }
            out.append(")")
        }
    }

    private fun writeAndEscape(value: String, out: StringBuilder, charsToEscape: String) {
        var result = value
        charsToEscape.forEach {
            result = AttStrUtils.replace(result, it.toString(), "\\$it")
        }
        out.append(result)
    }
}
