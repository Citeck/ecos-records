package ru.citeck.ecos.records3.record.atts.schema.read

import mu.KotlinLogging
import ru.citeck.ecos.commons.utils.NameUtils
import ru.citeck.ecos.commons.utils.StringUtils.isBlank
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.meta.util.AttStrUtils
import ru.citeck.ecos.records2.request.error.ErrorUtils
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.proc.AttProcDef
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.exception.AttSchemaException
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import ru.citeck.ecos.records3.utils.AttUtils
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors

class AttSchemaReader(services: RecordsServiceFactory) {

    companion object {

        private val log = KotlinLogging.logger {}

        private const val GQL_ONE_ARG_ATT = "^%s\\((n:)?['\"](.+?)['\"]\\)"
        private const val GQL_ONE_ARG_ATT_WITH_INNER = "$GQL_ONE_ARG_ATT\\s*\\{(.+)}"

        private val GQL_ATT_PATTERN = Pattern.compile(String.format(GQL_ONE_ARG_ATT_WITH_INNER, "atts?"))
        private val GQL_ATT_WITHOUT_INNER_PATTERN = Pattern.compile(String.format(GQL_ONE_ARG_ATT, "atts?"))

        private val GQL_EDGE_PATTERN = Pattern.compile(String.format(GQL_ONE_ARG_ATT_WITH_INNER, "edge"))
        private val GQL_AS_PATTERN = Pattern.compile(String.format(GQL_ONE_ARG_ATT_WITH_INNER, "as"))
        private val GQL_HAS_PATTERN = Pattern.compile(String.format(GQL_ONE_ARG_ATT, "has"))
        private val GQL_SIMPLE_AS_PATTERN = Pattern.compile("\\?as\\((n:)?[\\'\"](.+?)[\\'\"]\\)")

        private val FIXED_ROOT_ATTS_MAPPING = mapOf(
            Pair(".type{id}", "_type?id"),
            Pair("?type{id}", "_type?id")
        )

        private val ATT_NULL = SchemaAtt.create()
            .withName(RecordConstants.ATT_NULL)
            .withInner(
                SchemaAtt.create()
                    .withName(ScalarType.STR.schema)
                    .build()
            ).build()
    }

    private val procReader = services.attProcReader
    private val readerV2 = AttSchemaReaderV2(services)

    fun read(attributes: Collection<String>): List<SchemaAtt> {
        if (attributes.isEmpty()) {
            return emptyList()
        }
        return read(AttUtils.toMap(attributes))
    }

    fun read(attributes: Map<String, *>): List<SchemaAtt> {

        if (attributes.isEmpty()) {
            return emptyList()
        }

        val schemaAtts = ArrayList<SchemaAtt>()
        val readFunc = { key: String, value: Any? ->
            if (value is String) {
                schemaAtts.add(read(key, value))
            } else {
                schemaAtts.add(readObjAtt(key, value))
            }
        }

        val context = RequestContext.getCurrent()
        if (context == null) {
            attributes.forEach { (k, v) -> readFunc.invoke(k, v) }
        } else {
            attributes.forEach { (k, v) ->
                try {
                    readFunc.invoke(k, v)
                } catch (e: Exception) {
                    schemaAtts.add(ATT_NULL.withAlias(k))
                    context.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e) }
                }
            }
        }

        return schemaAtts
    }

    private fun readObjAtt(key: String, value: Any?): SchemaAtt {
        if (value == null) {
            return SchemaAtt.create {
                withName(RecordConstants.ATT_NULL)
                withInner(SchemaAtt.create { withName(ScalarType.STR.schema) })
            }
        }
        var attValue = value
        if (attValue is Collection<*>) {
            attValue = attValue.associateWith {
                if (it !is String) {
                    throw AttSchemaException("Incorrect object attribute. Collection should has only string values")
                }
                it
            }
        }
        if (attValue is Map<*, *>) {
            if (!attValue.keys.all { it is String }) {
                throw AttSchemaException("Incorrect object attribute. Map should has only string keys")
            }
            @Suppress("UNCHECKED_CAST")
            return readerV2.read(key, key, read(attValue as Map<String, *>), emptyList())
        }
        throw AttSchemaException(
            "Incorrect object attribute. " +
                "Expected Map or Collection but found ${value::class}"
        )
    }

    /**
     * @throws AttReadException when attribute can't be read
     */
    fun read(attribute: String): SchemaAtt {
        return read("", attribute)
    }

    fun read(alias: String, attribute: String): SchemaAtt {

        if (isBlank(attribute)) {
            throw AttReadException(alias, attribute, "Empty attribute")
        }

        val fixedAtt = FIXED_ROOT_ATTS_MAPPING[attribute] ?: attribute

        val isDotAtt = fixedAtt[0] == '.'
        var readErrorV2: AttSchemaException? = null
        if (!isDotAtt) {
            try {
                return readerV2.read(alias, attribute)
            } catch (error: AttSchemaException) {
                readErrorV2 = error
            }
        }

        val innerAtt = "\"${alias.replace("\"", "\\\"")}\":" + if (isDotAtt) {
            fixedAtt.substring(1)
        } else {
            fixedAtt
        }

        try {
            return readInnerRawAtt(
                innerAtt,
                isDotAtt,
                false
            ) { al, att, proc -> readInner(al, att, proc, emptyList()) }
        } catch (error: AttSchemaException) {
            if (readErrorV2 != null) {
                log.error("AttSchemaException: " + error.message)
                throw readErrorV2
            }
            throw error
        }
    }

    fun readInner(
        alias: String,
        attributeArg: String,
        processors: List<AttProcDef>,
        lastInnerAtts: List<SchemaAtt>
    ): SchemaAtt {

        var attribute = attributeArg

        if (attribute[0] == '#') {

            val questionIdx = attribute.indexOf('?')
            if (questionIdx == -1) {
                throw AttReadException(
                    alias,
                    attribute,
                    "Scalar type is mandatory for attributes with #. E.g.: #name?protected"
                )
            }
            if (attribute.endsWith("?options") || attribute.endsWith("?distinct")) {
                attribute += "{label:disp,value:str}"
            } else if (attribute.endsWith("?createVariants")) {
                attribute += "{json}"
            }
            attribute = (
                ".edge(n:\"" +
                    attribute.substring(1, questionIdx)
                        .replace("\"", "\\\"") + "\"){" +
                    attribute.substring(questionIdx + 1) + "}"
                )
        }
        return if (attribute[0] == '.') {
            readDotAtt(alias, attribute.substring(1), processors, lastInnerAtts)
        } else if (attribute.length > 2 && (attribute[0] == '"' || attribute[0] == '\'') && attribute[1] == '.') {
            attribute = AttStrUtils.removeQuotes(attribute)
            readDotAtt(alias, attribute.substring(1), processors, lastInnerAtts)
        } else {
            val innerScalarIdx = AttStrUtils.indexOf(attributeArg, '?')
            try {
                if (innerScalarIdx != -1) {
                    val attWithoutScalar = attributeArg.substring(0, innerScalarIdx)
                    val scalarStr = attributeArg.substring(innerScalarIdx)
                    ScalarType.getBySchema(scalarStr)
                        ?: throw AttReadException(alias, attribute, "Unknown scalar: $scalarStr")
                    val scalarSchema = SchemaAtt.create { withName(scalarStr) }
                    readerV2.read(alias, attWithoutScalar, listOf(scalarSchema), processors)
                } else {
                    readerV2.read(alias, attributeArg, lastInnerAtts, processors)
                }
            } catch (readErrorV2: AttSchemaException) {
                try {
                    readSimpleAtt(alias, attribute, processors, lastInnerAtts)
                } catch (readError: AttSchemaException) {
                    throw readErrorV2
                }
            }
        }
    }

    /* === PRIVATE === */

    private fun readInnerRawAtts(
        innerAtts: String,
        dotContext: Boolean,
        parserFunc: (
            alias: String,
            att: String,
            processors: List<AttProcDef>
        ) -> SchemaAtt
    ): List<SchemaAtt> {

        val innerAttsList = AttStrUtils.split(innerAtts, ",").filter { it.isNotBlank() }
        val multipleAtts = innerAttsList.size > 1
        return innerAttsList
            .stream()
            .map { att: String -> readInnerRawAtt(att, dotContext, multipleAtts, parserFunc) }
            .collect(Collectors.toList())
    }

    private fun readInnerRawAtt(
        innerAtt: String,
        dotContext: Boolean,
        multipleAtts: Boolean,
        parserFunc: (
            alias: String,
            att: String,
            processors: List<AttProcDef>
        ) -> SchemaAtt
    ): SchemaAtt {

        val attWithProc = procReader.read(innerAtt.trim())

        var att = attWithProc.attribute
        val aliasDelimIdx = AttStrUtils.indexOf(att, ":")

        var alias = ""
        if (aliasDelimIdx > 0) {
            alias = att.substring(0, aliasDelimIdx).replace("\\:", ":").trim()
            alias = AttStrUtils.removeQuotes(alias)
            alias = NameUtils.unescape(alias)
            att = att.substring(aliasDelimIdx + 1).replace("\\:", ":").trim()
        }

        if (alias.isEmpty() && multipleAtts) {
            alias = if (dotContext) {
                val braceIdx = att.indexOf('{')
                if (braceIdx > 0) att.substring(0, braceIdx) else att
            } else {
                val questionIdx = att.indexOf('?')
                if (questionIdx > 0) {
                    att.substring(0, questionIdx)
                } else {
                    att
                }
            }
        }

        return parserFunc.invoke(
            alias.trim(),
            if (dotContext) { ".$att" } else { att },
            attWithProc.processors
        )
    }

    private fun readLastSimpleAtt(
        alias: String,
        attributeArg: String,
        processors: List<AttProcDef>,
        lastInnerAtts: List<SchemaAtt>
    ): SchemaAtt {

        var attribute = attributeArg

        if (attribute[0] == '?') {
            attribute = attribute.substring(1)
            if (attribute.startsWith("has(")) {
                return parseHasAtt(alias, attribute, processors)
            } else if (attribute.startsWith("as(")) {
                return parseAsAtt(alias, attribute, processors, lastInnerAtts)
            }
            return SchemaAtt.create()
                .withAlias(alias)
                .withProcessors(processors)
                .withName("?$attribute")
                .build()
        }

        var att = attribute
        val questionIdx = AttStrUtils.indexOf(att, "?")
        if (questionIdx != -1) {
            att = att.substring(0, questionIdx) + "{?" + att.substring(questionIdx + 1) + "}"
        }

        val arrIdx = AttStrUtils.indexOf(att, "[]")
        var isMultiple = false
        if (arrIdx != -1) {
            isMultiple = true
            att = att.substring(0, arrIdx) + att.substring(arrIdx + 2)
        }

        var attName = att
        var innerAttsStr: String? = null
        val openBraceIdx = AttStrUtils.indexOf(attName, '{')

        if (openBraceIdx > -1) {
            attName = attName.substring(0, openBraceIdx)
            val closeBraceIdx = AttStrUtils.indexOf(att, '}')
            if (closeBraceIdx == -1) {
                throw AttReadException(alias, att, "Missing close bracket: '}'")
            }
            innerAttsStr = att.substring(openBraceIdx + 1, closeBraceIdx)
        }

        val resInnerAtts: List<SchemaAtt>
        resInnerAtts = if (innerAttsStr == null) {
            if (lastInnerAtts.isNotEmpty()) {
                lastInnerAtts
            } else {
                listOf(
                    SchemaAtt.create()
                        .withName("?disp")
                        .build()
                )
            }
        } else {
            readInnerRawAtts(innerAttsStr, false) {
                    al, innerAtt, proc ->
                readInner(al, innerAtt, proc, lastInnerAtts)
            }
        }
        return SchemaAtt.create()
            .withAlias(alias)
            .withName(AttStrUtils.removeQuotes(attName))
            .withMultiple(isMultiple)
            .withProcessors(processors)
            .withInner(resInnerAtts)
            .build()
    }

    private fun readSimpleAtt(
        alias: String,
        attributeArg: String,
        processors: List<AttProcDef>,
        lastInnerAtts: List<SchemaAtt>
    ): SchemaAtt {

        val firstAttChar = attributeArg[0]
        if (firstAttChar != '_' &&
            firstAttChar != '?' &&
            firstAttChar != '\'' &&
            firstAttChar != '"' &&
            firstAttChar != '$' &&
            !Character.isLetterOrDigit(firstAttChar)
        ) {
            throw AttReadException(alias, attributeArg, "Incorrect attribute")
        }

        var attribute = attributeArg

        if (attribute.contains("?as(")) {
            var matcher = GQL_SIMPLE_AS_PATTERN.matcher(attribute)
            while (matcher.find()) {
                attribute = attribute.replace(matcher.group(0), "._as." + matcher.group(2))
                matcher = GQL_SIMPLE_AS_PATTERN.matcher(attribute)
            }
        }
        val dotPath = AttStrUtils.split(attribute, '.').map {
            AttStrUtils.removeQuotes(AttStrUtils.replace(it, "\\.", "."))
        }
        val lastPathElem = dotPath[dotPath.size - 1]

        var schemaAtt = readLastSimpleAtt(
            if (dotPath.size == 1) alias else "",
            lastPathElem,
            if (dotPath.size == 1) processors else emptyList(),
            lastInnerAtts
        )

        for (i in dotPath.size - 2 downTo 0) {
            var pathElem = dotPath[i]
            val arrIdx = AttStrUtils.indexOf(pathElem, "[]")
            var isMultiple = false
            if (arrIdx != -1) {
                isMultiple = true
                pathElem = pathElem.substring(0, arrIdx) + pathElem.substring(arrIdx + 2)
            }
            schemaAtt = SchemaAtt.create()
                .withAlias(if (i == 0) alias else "")
                .withProcessors(if (i == 0) processors else emptyList())
                .withName(AttStrUtils.removeQuotes(pathElem))
                .withMultiple(isMultiple)
                .withInner(schemaAtt)
                .build()
        }

        return schemaAtt
    }

    /**
     * @param attribute - attribute without dot
     */
    private fun readDotAtt(
        alias: String,
        attribute: String,
        processors: List<AttProcDef>,
        lastInnerAtts: List<SchemaAtt>
    ): SchemaAtt {

        if (attribute.startsWith("att")) {
            return parseGqlAtt(alias, attribute, processors, lastInnerAtts)
        }
        if (attribute.startsWith("edge")) {
            return readEdgeAtt(alias, attribute, processors, lastInnerAtts)
        }
        val braceIdx = AttStrUtils.indexOf(attribute, '(')
        if (braceIdx == -1) {
            return SchemaAtt.create()
                .withAlias(alias)
                .withProcessors(processors)
                .withName("?$attribute")
                .build()
        }
        if (attribute.startsWith("as")) {
            return parseAsAtt(alias, attribute, processors, lastInnerAtts)
        }
        if (attribute.startsWith("has")) {
            return parseHasAtt(alias, attribute, processors)
        }
        throw AttReadException(alias, attribute, "Unknown dot attribute")
    }

    private fun parseAsAtt(
        alias: String,
        attribute: String,
        processors: List<AttProcDef>,
        lastInnerAtts: List<SchemaAtt>
    ): SchemaAtt {

        val matcher = GQL_AS_PATTERN.matcher(attribute)
        if (!matcher.matches()) {
            throw AttReadException(alias, attribute, "Incorrect 'as' attribute")
        }
        val attName = matcher.group(2)
        val attInner = matcher.group(3)
        return SchemaAtt.create()
            .withAlias(alias)
            .withName(RecordConstants.ATT_AS)
            .withProcessors(processors)
            .withInner(
                SchemaAtt.create()
                    .withName(attName)
                    .withInner(
                        readInnerRawAtts(attInner, true) {
                                al, att, proc ->
                            readInner(al, att, proc, lastInnerAtts)
                        }
                    )
            ).build()
    }

    private fun parseHasAtt(alias: String, attribute: String, processors: List<AttProcDef>): SchemaAtt {
        val matcher = GQL_HAS_PATTERN.matcher(attribute)
        if (!matcher.matches()) {
            throw AttReadException(alias, attribute, "Incorrect 'has' attribute")
        }
        val attName = matcher.group(2)
        return SchemaAtt.create()
            .withAlias(alias)
            .withName(RecordConstants.ATT_HAS)
            .withProcessors(processors)
            .withInner(
                SchemaAtt.create()
                    .withName(attName)
                    .withInner(SchemaAtt.create().withName("?bool"))
            ).build()
    }

    private fun readEdgeAtt(
        alias: String,
        attribute: String,
        processors: List<AttProcDef>,
        innerAtts: List<SchemaAtt>
    ): SchemaAtt {

        val matcher = GQL_EDGE_PATTERN.matcher(attribute)
        if (!matcher.matches()) {
            throw AttReadException(alias, attribute, "Incorrect 'edge' attribute")
        }

        val attName = matcher.group(2)
        val attInner = matcher.group(3)

        val innerAttsStr = AttStrUtils.split(attInner, ",")
        val schemaInnerAtts: MutableList<SchemaAtt> = ArrayList()
        val multipleAtts = innerAttsStr.size > 1
        for (innerAttWithAlias in innerAttsStr) {
            schemaInnerAtts.add(
                readInnerRawAtt(
                    innerAttWithAlias,
                    false,
                    multipleAtts
                ) { al, att, proc -> parseEdgeInnerAtt(al, att, proc, innerAtts) }
            )
        }
        return SchemaAtt.create()
            .withAlias(alias)
            .withProcessors(processors)
            .withName(RecordConstants.ATT_EDGE)
            .withInner(SchemaAtt.create().withName(AttStrUtils.removeEscaping(attName)).withInner(schemaInnerAtts))
            .build()
    }

    private fun parseEdgeInnerAtt(
        alias: String,
        attribute: String,
        processors: List<AttProcDef>,
        innerAtts: List<SchemaAtt>
    ): SchemaAtt {

        val attBuilder = SchemaAtt.create()
            .withAlias(alias)
            .withName(attribute)

        when (attribute) {
            "name",
            "title",
            "description",
            "javaClass",
            "editorKey",
            "type" ->
                return attBuilder.withInner(
                    SchemaAtt.create().withName("?str")
                ).withProcessors(processors)
                    .build()
            "protected",
            "unreadable",
            "multiple",
            "isAssoc" ->
                return attBuilder
                    .withProcessors(processors)
                    .withInner(SchemaAtt.create().withName("?bool")).build()
        }

        val openBraceIdx = attribute.indexOf('{')
        val closeBraceIdx = attribute.lastIndexOf('}')

        if (openBraceIdx == -1 || closeBraceIdx == -1 || openBraceIdx > closeBraceIdx) {
            throw AttSchemaException("Incorrect edge inner attribute: '$attribute'")
        }

        val attWithoutInner = attribute.substring(0, openBraceIdx)
        val attInnerAtts = attribute.substring(openBraceIdx + 1, closeBraceIdx)
        val multiple = "val" != attWithoutInner

        when (attWithoutInner) {
            "val",
            "vals",
            "options",
            "distinct",
            "createVariants" ->
                return attBuilder.withName(attWithoutInner)
                    .withMultiple(multiple)
                    .withInner(
                        readInnerRawAtts(attInnerAtts, true) {
                                al, att, proc ->
                            this.readInner(al, att, proc, innerAtts)
                        }
                    )
                    .withProcessors(processors)
                    .build()
        }

        throw AttReadException(alias, attribute, "Unknown 'edge inner' attribute")
    }

    private fun parseGqlAtt(
        alias: String,
        attribute: String,
        processors: List<AttProcDef>,
        innerAtts: List<SchemaAtt>
    ): SchemaAtt {

        var gqlInnerAtts: List<SchemaAtt> = emptyList()
        var attName: String? = null
        val multiple = attribute.startsWith("atts")
        var matcher = GQL_ATT_PATTERN.matcher(attribute)

        if (!matcher.matches()) {
            if (innerAtts.isNotEmpty()) {
                matcher = GQL_ATT_WITHOUT_INNER_PATTERN.matcher(attribute)
                if (matcher.matches()) {
                    attName = matcher.group(2)
                    gqlInnerAtts = innerAtts
                }
            }
        } else {
            attName = matcher.group(2)
            gqlInnerAtts = readInnerRawAtts(matcher.group(3), true) {
                    al, att, proc ->
                readInner(al, att, proc, innerAtts)
            }
        }
        if (attName == null || gqlInnerAtts.isEmpty()) {
            throw AttReadException(alias, attribute, "Incorrect attribute")
        }
        return SchemaAtt.create()
            .withAlias(alias)
            .withName(AttStrUtils.removeEscaping(attName))
            .withMultiple(multiple)
            .withProcessors(processors)
            .withInner(gqlInnerAtts)
            .build()
    }
}
