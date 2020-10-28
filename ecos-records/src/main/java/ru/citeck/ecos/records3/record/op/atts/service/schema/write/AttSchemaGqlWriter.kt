package ru.citeck.ecos.records3.record.op.atts.service.schema.write

import ru.citeck.ecos.commons.utils.NameUtils
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt

/**
 * Writer to convert schema to legacy GraphQL format.
 */
class AttSchemaGqlWriter : AttSchemaWriter {

    override fun write(attribute: SchemaAtt, out: StringBuilder) {
        out.append(".")
        writeInner(attribute, out)
    }

    private fun writeInner(attribute: SchemaAtt, sb: StringBuilder) {
        writeInnerAtt(attribute, sb)
        for (processor in attribute.processors) {
            sb.append("|").append(processor.type).append("(")
            val argsSize = processor.arguments.size
            for (i in 0 until argsSize) {
                sb.append(processor.arguments[i].toString())
                if (i < argsSize - 1) {
                    sb.append(',')
                }
            }
            sb.append(")")
        }
    }

    private fun writeInnerAtt(attribute: SchemaAtt, sb: StringBuilder) {

        val alias: String = attribute.alias
        val name: String = attribute.name

        if (sb.length > 1 && alias.isNotEmpty()) {
            sb.append(NameUtils.escape(alias)).append(":")
        }
        if (attribute.isScalar()) {
            sb.append(name, 1, name.length)
            return
        }
        if (name[0] == '_' && attribute.inner.size == 1) {
            if (name == RecordConstants.ATT_EDGE) {
                writeEdge(attribute, sb)
                return
            }
            if (name == RecordConstants.ATT_AS) {
                writeAs(attribute, sb)
                return
            }
            if (name == RecordConstants.ATT_HAS) {
                writeHas(attribute, sb)
                return
            }
        }
        sb.append("att")
        if (attribute.multiple) {
            sb.append("s")
        }
        sb.append("(n:\"")
            .append(name.replace("\"", "\\\""))
            .append("\"){")

        val inner = attribute.inner

        for (i in inner.indices) {
            writeInner(inner[i], sb)
            if (i < inner.size - 1) {
                sb.append(",")
            }
        }
        sb.append("}")
    }

    private fun writeEdge(attribute: SchemaAtt, sb: StringBuilder) {

        val innerAtt: SchemaAtt = attribute.inner[0]

        sb.append("edge(n:\"")
            .append(innerAtt.name.replace("\"", "\\\""))
            .append("\"){")

        for (att in innerAtt.inner) {
            writeEdgeInner(att, sb)
            sb.append(",")
        }

        if (innerAtt.inner.isNotEmpty()) {
            sb.setLength(sb.length - 1)
        }

        sb.append("}")
    }

    private fun writeEdgeInner(attribute: SchemaAtt, sb: StringBuilder) {

        val alias: String = attribute.alias

        if (alias.isNotEmpty()) {
            sb.append(NameUtils.escape(alias)).append(":")
        }

        sb.append(attribute.name)

        when (attribute.name) {
            "val",
            "vals",
            "options",
            "distinct",
            "createVariants" -> {
                sb.append("{")
                for (att in attribute.inner) {
                    writeInner(att, sb)
                    sb.append(",")
                }
                if (attribute.inner.isNotEmpty()) {
                    sb.setLength(sb.length - 1)
                }
                sb.append("}")
            }
        }
    }

    private fun writeAs(attribute: SchemaAtt, sb: StringBuilder) {
        val innerAtt: SchemaAtt = attribute.inner[0]
        sb.append("as(n:\"")
            .append(innerAtt.name.replace("\"", "\\\""))
            .append("\"){")
        for (att in innerAtt.inner) {
            writeInner(att, sb)
        }
        sb.append("}")
    }

    private fun writeHas(attribute: SchemaAtt, sb: StringBuilder) {
        sb.append("has(n:\"")
            .append(attribute.inner[0].name.replace("\"", "\\\""))
            .append("\")")
    }
}
