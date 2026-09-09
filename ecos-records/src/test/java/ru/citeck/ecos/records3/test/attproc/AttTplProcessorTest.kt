package ru.citeck.ecos.records3.test.attproc

import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.proc.AttTplProcessor
import ru.citeck.ecos.records3.record.atts.schema.read.proc.AttProcReader
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttTplProcessorTest {

    companion object {
        private const val SOURCE_ID = "test-src"
        private const val REC_ID = "rec-1"
    }

    private val services = RecordsServiceFactory()
    private val records = services.recordsService

    private val recordRef = EntityRef.create(SOURCE_ID, REC_ID)

    init {
        records.register(object : RecordAttsDao {
            override fun getId() = SOURCE_ID
            override fun getRecordAtts(recordId: String): Any = TestRecord()
        })
    }

    private fun getAtt(att: String) = records.getAtt(recordRef, att)

    private fun recordId(): String = getAtt("?id").asText()

    @Test
    fun literalTemplateTest() {
        assertEquals(
            "https://host/v2/dashboard?recordRef=${recordId()}&ws=my-ws",
            getAtt("?id|tpl('https://host/v2/dashboard?recordRef={{_}}&ws={{_workspace?localId}}')").asText()
        )
    }

    @Test
    fun webUrlTemplateTest() {
        assertEquals(
            "http://localhost/v2/dashboard?recordRef=${recordId()}",
            getAtt("?id|tpl('{{\$webUrl}}v2/dashboard?recordRef={{_}}')").asText()
        )
    }

    @Test
    fun selfPlaceholderTest() {
        assertEquals("Hello, Ivan!", getAtt("name|tpl('Hello, {{_}}!')").asText())
    }

    @Test
    fun templateWithoutSelfPlaceholderTest() {
        assertEquals("ws=my-ws", getAtt("name|tpl('ws={{_workspace?localId}}')").asText())
    }

    @Test
    fun nullValueTest() {
        assertTrue(getAtt("unknownField|tpl('value: {{_}}')").isNull())
    }

    @Test
    fun emptyValueTest() {

        // empty string is a normal value and template is applied to it
        assertEquals("value: ", getAtt("emptyStr|tpl('value: {{_}}')").asText())
        assertEquals(listOf("v=aa", "v=", "v=bb"), getAtt("multiWithEmpty[]|tpl('v={{_}}')").map { it.asText() })

        // or-else before the processor may be used to replace empty value
        assertEquals("value: none", getAtt("emptyStr!'none'|tpl('value: {{_}}')").asText())
    }

    @Test
    fun nullAttInTemplateTest() {
        assertEquals("[]", getAtt("name|tpl('[{{unknownField}}]')").asText())
    }

    @Test
    fun dollarPlaceholdersTest() {
        assertEquals("Ivan from my-ws", getAtt("name|tpl('\${_} from \${_workspace?localId}')").asText())
    }

    @Test
    fun escapedPlaceholderTest() {
        assertEquals("{{_}} is Ivan", getAtt("name|tpl('\\{{_}} is {{_}}')").asText())
    }

    @Test
    fun wholeTemplateIsPlaceholderTest() {

        // template with single placeholder returns value as is
        val value = getAtt("name|tpl('{{num?raw}}')")
        assertTrue(value.isNumber(), "value is not a number: $value")
        assertEquals(42, value.asInt())

        // attributes are loaded with default scalar type, so ?raw is required to keep the type
        assertEquals("42", getAtt("name|tpl('{{num}}')").asText())
    }

    @Test
    fun arrayValueTest() {
        val value = getAtt("multi[]|tpl('v={{_}}')")
        assertTrue(value.isArray(), "value is not an array: $value")
        assertEquals(listOf("v=aa", "v=bb"), value.map { it.asText() })
    }

    @Test
    fun computedTemplateTest() {
        assertEquals(
            "ref=${recordId()} ws=my-ws other={name}",
            getAtt("?id|tpl(a:tpl, '_workspace?localId')").asText()
        )
    }

    @Test
    fun computedTemplateWithDollarPlaceholdersTest() {
        assertEquals(
            "ws=my-ws other={name}",
            getAtt("?id|tpl(a:tplDollar, '_workspace?localId')").asText()
        )
    }

    @Test
    fun computedObjectTemplateTest() {

        val value = getAtt("name|tpl(a:tplObj?json, 'name')")

        assertTrue(value.isObject(), "value is not an object: $value")
        assertEquals("Ivan", value["a"].asText())
        assertEquals("x-Ivan", value["b"].asText())
        assertEquals("{unknownField}", value["c"].asText())
    }

    @Test
    fun nullTemplateTest() {
        assertTrue(getAtt("?id|tpl(a:unknownTpl)").isNull())
    }

    @Test
    fun aliasesTest() {
        assertEquals(
            "ws=my-ws name=Ivan",
            getAtt("?id|tpl(a:tplWithAliases, 'ws:_workspace?localId', 'n:name')").asText()
        )
    }

    @Test
    fun aliasesWithProcessorsInAttTest() {
        assertEquals(
            "ws=a:b-my-ws name=IVAN",
            getAtt(
                "?id|tpl(a:tplWithAliases, 'ws:_workspace?localId|presuf(\"a:b-\")', 'n:name|uppercase()')"
            ).asText()
        )
    }

    @Test
    fun aliasOverridesAttFromLiteralTemplateTest() {
        assertEquals("u=my-ws", getAtt("?id|tpl('u={{u}}', 'u:_workspace?localId')").asText())
    }

    @Test
    fun orElseAfterTplTest() {
        assertEquals("none", getAtt("unknownField|tpl('value: {{_}}')!'none'").asText())
    }

    @Test
    fun multiProcTest() {
        assertEquals("NAME: IVAN", getAtt("name|tpl('name: {{_}}')|uppercase()").asText())
    }

    @Test
    fun readerTest() {

        val attWithProc = AttProcReader().read("?id|tpl('a{{b?disp}}c')")
        assertEquals("?id", attWithProc.attribute)
        assertEquals(1, attWithProc.processors.size)

        val procDef = attWithProc.processors[0]
        assertEquals("tpl", procDef.type)
        assertEquals("a{{b?disp}}c", procDef.arguments[0].asText())

        val processor = AttTplProcessor()
        assertEquals(setOf("b?disp"), processor.getAttsToLoad(procDef.arguments).toSet())
    }

    @Test
    fun attsToLoadForComputedTemplateTest() {

        val procDef = AttProcReader().read(
            "?id|tpl(a:tplAtt, 'name', 'ws:_workspace?localId|presuf(\"a:b-\")')"
        ).processors[0]

        assertEquals(
            setOf("tplAtt", "name", "_workspace?localId|presuf(\"a:b-\")"),
            AttTplProcessor().getAttsToLoad(procDef.arguments).toSet()
        )
    }

    class TestRecord : AttValue {
        override fun getAtt(name: String): Any? {
            return when (name) {
                "_workspace" -> EntityRef.valueOf("emodel/workspace@my-ws")
                "name" -> "Ivan"
                "num" -> 42
                "multi" -> listOf("aa", "bb")
                "emptyStr" -> ""
                "multiWithEmpty" -> listOf("aa", "", "bb")
                "tpl" -> "ref={{_}} ws={{_workspace?localId}} other={{name}}"
                "tplDollar" -> "ws=\${_workspace?localId} other=\${name}"
                "tplWithAliases" -> "ws={{ws}} name={{n}}"
                "tplObj" -> ObjectData.create(
                    mapOf(
                        "a" to "{{_}}",
                        "b" to "x-{{name}}",
                        "c" to "{{unknownField}}"
                    )
                )
                else -> null
            }
        }
    }
}
