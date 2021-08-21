package ru.citeck.ecos.records3.record.atts.schema.read

import ru.citeck.ecos.records2.meta.util.AttStrUtils
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.proc.AttProcDef
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt

class AttSchemaReaderV2(services: RecordsServiceFactory) {

    companion object {
        private val SCALAR_ATTS_LIST = ScalarType.values().associate {
            it.schema to listOf(SchemaAtt.create().withName(it.schema).build())
        }
    }

    private val procReader = services.attProcReader

    fun read(alias: String, attToRead: String): SchemaAtt {

        if (attToRead.isBlank()) {
            throw AttReadException(alias, attToRead, "Attribute is blank")
        }

        val attWithProc = procReader.read(attToRead.trim())
        val attribute = attWithProc.attribute

        val innerPartBegin = AttStrUtils.indexOf(attribute, '{')
        if (innerPartBegin == 0) {
            throw AttReadException(alias, attToRead, "Inner part can't be started without attribute path")
        }
        val attPathEndIdx: Int
        val innerAtts = if (innerPartBegin != -1) {
            val lastNotSpaceCharIdx = AttStrUtils.getLastNonWhitespaceCharIdx(attribute)
            val lastChar = attribute[lastNotSpaceCharIdx]
            if (lastChar != '}') {
                throw AttReadException(alias, attToRead, "Attribute with inner part should ends with '}'")
            }
            attPathEndIdx = innerPartBegin
            readInnerAtts(attribute.substring(innerPartBegin + 1, lastNotSpaceCharIdx).trim())
        } else {
            val scalarIdx = AttStrUtils.indexOf(attribute, '?')
            if (scalarIdx != -1) {
                val scalar = attribute.substring(scalarIdx)
                attPathEndIdx = scalarIdx
                SCALAR_ATTS_LIST[scalar] ?: throw AttReadException(alias, attToRead, "Unknown scalar: '$scalar'")
            } else {
                attPathEndIdx = attribute.length
                SCALAR_ATTS_LIST[ScalarType.DISP.schema] ?: error("${ScalarType.DISP.schema} schema doesn't found")
            }
        }
        if (attPathEndIdx == 0) {
            if (innerAtts.size == 1) {
                return if (alias.isNotBlank() || attWithProc.processors.isNotEmpty()) {
                    val attCopy = innerAtts[0].copy()
                    if (alias.isNotBlank()) {
                        attCopy.withAlias(alias)
                    }
                    if (attWithProc.processors.isNotEmpty()) {
                        attCopy.withProcessors(attWithProc.processors)
                    }
                    attCopy.build()
                } else {
                    innerAtts[0]
                }
            }
            throw AttReadException(alias, attToRead, "Attribute path last index doesn't found")
        }
        return read(alias, attribute.substring(0, attPathEndIdx), innerAtts, attWithProc.processors)
    }

    fun read(alias: String, attToRead: String, innerAtts: List<SchemaAtt>): SchemaAtt {

        if (attToRead.isBlank()) {
            throw AttReadException(alias, attToRead, "Attribute is blank")
        }

        val attWithProc = procReader.read(attToRead.trim())
        val path = attWithProc.attribute

        return read(alias, path, innerAtts, attWithProc.processors)
    }

    fun read(alias: String, path: String, innerAtts: List<SchemaAtt>, processors: List<AttProcDef>): SchemaAtt {

        if (innerAtts.isEmpty()) {
            throw AttReadException(alias, path, "Inner atts is empty")
        }

        val pathParts = split(path, '.')

        return if (pathParts.size == 1) {

            pathPartToAttBuilder(pathParts[0])
                .withInner(innerAtts)
                .withAlias(alias)
                .withProcessors(processors)
                .build()
        } else {

            var schemaAtt = pathPartToAttBuilder(pathParts.last())
                .withInner(innerAtts)
                .build()

            for (i in (pathParts.lastIndex - 1) downTo 0) {

                val nextAtt = pathPartToAttBuilder(pathParts[i])
                    .withInner(schemaAtt)
                if (i == 0) {
                    nextAtt.withAlias(alias)
                        .withProcessors(processors)
                }
                schemaAtt = nextAtt.build()
            }
            schemaAtt
        }
    }

    private fun pathPartToAttBuilder(part: String): SchemaAtt.Builder {
        val builder = SchemaAtt.create()
        if (part.endsWith("[]")) {
            builder.withMultiple(true)
            builder.withName(part.substring(0, part.length - 2))
        } else {
            builder.withName(part)
        }
        return builder
    }

    private fun readInnerAtts(innerAttsStr: String): List<SchemaAtt> {

        return split(innerAttsStr, ',').map {

            val aliasDelimIdx = AttStrUtils.indexOf(it, ':')
            val dotIdx = AttStrUtils.indexOf(it, '.')

            if (aliasDelimIdx == -1 || dotIdx != -1 && dotIdx < aliasDelimIdx) {

                val dotsMaxEscapeIdx = if (dotIdx != -1) {
                    dotIdx
                } else {
                    val braceIdx = it.indexOf("{")
                    if (braceIdx != -1) {
                        braceIdx
                    } else {
                        it.length
                    }
                }

                var escapedDotsIdx = AttStrUtils.indexOf(it, "\\:")
                var attWithoutEscapedDots = it
                while (escapedDotsIdx != -1 && escapedDotsIdx < dotsMaxEscapeIdx) {
                    attWithoutEscapedDots = attWithoutEscapedDots.substring(0, escapedDotsIdx) +
                        attWithoutEscapedDots.substring(escapedDotsIdx + 1)
                    escapedDotsIdx = AttStrUtils.indexOf(attWithoutEscapedDots, "\\:")
                }
                read("", attWithoutEscapedDots)
            } else {
                val alias = it.substring(0, aliasDelimIdx).trim()
                val attribute = it.substring(aliasDelimIdx + 1).trim()
                read(alias, attribute)
            }
        }
    }

    private fun split(value: String, delimiter: Char): List<String> {
        return AttStrUtils.split(value, delimiter).map {
            AttStrUtils.replace(it.trim(), "\\$delimiter", delimiter.toString())
        }
    }
}
