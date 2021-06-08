package ru.citeck.ecos.records3.test.schema

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.proc.AttProcDef
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.RequestContext.Companion.doWithCtx

class SchemaTest {

    private val factory = RecordsServiceFactory()
    private val reader = factory.attSchemaReader
    private val writer = factory.attSchemaWriter

    @Test
    fun innerAliasTest() {
        val schema = ".edge(n:\"test\"){" +
            "distinct{str,disp}," +
            "options{str,disp}," +
            "createVariants{json}" +
            "}"
        val (_, _, _, inner1) = reader.read(schema)
        val inner = inner1[0].inner
        assertEquals(3, inner.size)
        assertEquals("distinct{str,disp}", inner[0].alias)
        assertEquals("options{str,disp}", inner[1].alias)
        assertEquals("createVariants{json}", inner[2].alias)
    }

    @Test
    fun edgeGqlTest() {
        assertEdgeMetaVal("vals", true)
        assertEdgeMetaVal("val", false)
        assertEdgeMetaVal("options", true)
        assertEdgeMetaVal("distinct", true)
        assertEdgeMetaVal("createVariants", true)
        assertEdgeScalar("protected", "bool")
        assertEdgeScalar("unreadable", "bool")
        assertEdgeScalar("multiple", "bool")
        assertEdgeScalar("isAssoc", "bool")
        assertEdgeScalar("name", "str")
        assertEdgeScalar("title", "str")
        assertEdgeScalar("description", "str")
        assertEdgeScalar("javaClass", "str")
        assertEdgeScalar("editorKey", "str")
        assertEdgeScalar("type", "str")
        assertAtt("_edge.cm:name.protected?bool", ".edge(n:\"cm:name\"){protected}")
    }

    private fun assertEdgeMetaVal(inner: String, multiple: Boolean) {
        val edgeName = "cm:name"
        val innerInner = "att(n:\"title\"){disp}"
        val att = ".edge(n:\"$edgeName\"){$inner{$innerInner}}"
        val multiplePostfix = if (multiple) { "[]" } else { "" }
        assertAtt("_edge.$edgeName.$inner$multiplePostfix.title", att)
    }

    private fun assertEdgeScalar(inner: String, innerInner: String) {
        val edgeName = "cm:name"
        val att = ".edge(n:\"$edgeName\"){$inner}"
        val (_, _, _, inner1) = reader.read(att)
        assertEquals(1, inner1.size)
        assertEquals(1, inner1[0].inner.size)
        assertEquals(inner, inner1[0].inner[0].name)
        val lastAtt = if (innerInner == "disp") { "" } else { "?$innerInner" }
        assertAtt("_edge.$edgeName.$inner$lastAtt", att)
    }

    @Test
    fun edgeSimpleTest() {
        assertAtt("_edge.cm:name.protected?bool", "#cm:name?protected")
    }

    @Test
    fun hasAttTest() {
        assertAtt("permissions._has.Write?bool", "permissions?has('Write')")
    }

    @Test
    fun asAttTest() {
        assertAtt(
            "permissions._as.NodeRef.inner.att?str",
            ".att(n:\"permissions\"){as('NodeRef'){att('inner'){att('att'){str}}}}"
        )
    }

    @Test
    fun objAttTest() {
        assertAtt(
            "cm:name{first?num,second?str,third}",
            "cm:name{first?num,second?str,third}"
        )
    }

    @Test
    fun attAsMapTest() {

        val atts = mapOf(
            "simple" to mapOf(
                "firstAlias" to "cm:name?str"
            ),
            "inner_att.without.array" to mapOf(
                "secondAlias" to "cm:title?str"
            ),
            "inner_att.with.array[]" to mapOf(
                "thirdAlias" to "cm:number?num"
            ),
            "inner_att.with.array[].inner" to mapOf(
                "fourthAlias" to "cm:number2?num"
            )
        )
        val parsedAtts = reader.read(atts)
        assertThat(parsedAtts).hasSize(atts.size)

        val attsAsMap = HashMap(writer.writeToMap(parsedAtts))
        assertThat(attsAsMap).isEqualTo(
            hashMapOf(
                "simple" to "simple{firstAlias:cm:name?str}",
                "inner_att.without.array" to "inner_att.without.array{secondAlias:cm:title?str}",
                "inner_att.with.array[]" to "inner_att.with.array[]{thirdAlias:cm:number?num}",
                "inner_att.with.array[].inner" to "inner_att.with.array[].inner{fourthAlias:cm:number2?num}"
            )
        )

        assertThat(parsedAtts.find { it.getAliasForValue() == "simple" }).isEqualTo(
            SchemaAtt.create {
                withName("simple")
                withInner(
                    listOf(
                        SchemaAtt.create()
                            .withAlias("firstAlias")
                            .withName("cm:name")
                            .withInner(SchemaAtt.create().withName(ScalarType.STR.schema))
                            .build()
                    )
                )
            }
        )
        assertThat(parsedAtts.find { it.getAliasForValue() == "inner_att.without.array" }).isEqualTo(
            SchemaAtt.create {
                withName("inner_att")
                withAlias("inner_att.without.array")
                withInner(
                    listOf(
                        SchemaAtt.create()
                            .withName("without")
                            .withInner(
                                listOf(
                                    SchemaAtt.create()
                                        .withName("array")
                                        .withInner(
                                            SchemaAtt.create()
                                                .withAlias("secondAlias")
                                                .withName("cm:title")
                                                .withInner(SchemaAtt.create().withName(ScalarType.STR.schema))
                                                .build()
                                        )
                                        .build()
                                )
                            )
                            .build()
                    )
                )
            }
        )
        assertThat(parsedAtts.find { it.getAliasForValue() == "inner_att.with.array[]" }).isEqualTo(
            SchemaAtt.create {
                withName("inner_att")
                withAlias("inner_att.with.array[]")
                withInner(
                    listOf(
                        SchemaAtt.create()
                            .withName("with")
                            .withInner(
                                listOf(
                                    SchemaAtt.create()
                                        .withName("array")
                                        .withMultiple(true)
                                        .withInner(
                                            SchemaAtt.create()
                                                .withAlias("thirdAlias")
                                                .withName("cm:number")
                                                .withInner(SchemaAtt.create().withName(ScalarType.NUM.schema))
                                                .build()
                                        )
                                        .build()
                                )
                            )
                            .build()
                    )
                )
            }
        )
        assertThat(parsedAtts.find { it.getAliasForValue() == "inner_att.with.array[].inner" }).isEqualTo(
            SchemaAtt.create {
                withName("inner_att")
                withAlias("inner_att.with.array[].inner")
                withInner(
                    listOf(
                        SchemaAtt.create()
                            .withName("with")
                            .withInner(
                                listOf(
                                    SchemaAtt.create()
                                        .withName("array")
                                        .withMultiple(true)
                                        .withInner(
                                            SchemaAtt.create()
                                                .withName("inner")
                                                .withInner(
                                                    SchemaAtt.create()
                                                        .withAlias("fourthAlias")
                                                        .withName("cm:number2")
                                                        .withInner(SchemaAtt.create().withName(ScalarType.NUM.schema))
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .build()
                                )
                            )
                            .build()
                    )
                )
            }
        )
    }

    @Test
    fun attWithProcTest() {

        assertAtt(
            "documents[]{name:cm:name|or('a:cm:title')|or('no-name')}",
            "documents[]{name:cm:name|or('a:cm:title')!'no-name'}",
            SchemaAtt.create {
                withName("documents")
                withMultiple(true)
                withInner(
                    SchemaAtt.create {
                        this.withAlias("name")
                        this.withName("cm:name")
                        this.withInner(SchemaAtt.create { withName("?disp") })
                        this.withProcessors(
                            listOf(
                                AttProcDef("or", listOf(DataValue.createStr("a:cm:title"))),
                                AttProcDef("or", listOf(DataValue.createStr("no-name")))
                            )
                        )
                    }
                )
            }
        )
    }

    @Test
    fun writeToMapTest() {
        val att = reader.read("field")
        val map = writer.writeToMap(listOf(att))
        assertThat(map).hasSize(1)
        assertThat(map["field"]).isEqualTo("field")
    }

    @Test
    fun attTest2() {

        assertAtt(
            "path0[].path1.INNER|proc0('arg0','arg1')|proc1('arg0','arg1')",
            "path0[].path1{INNER}|proc0('arg0','arg1')|proc1('arg0','arg1')",
            SchemaAtt.create()
                .withName("path0")
                .withMultiple(true)
                .withProcessors(listOf(
                    AttProcDef("proc0", listOf("arg0", "arg1").map { DataValue.createStr(it) }),
                    AttProcDef("proc1", listOf("arg0", "arg1").map { DataValue.createStr(it) })
                )).withInner(SchemaAtt.create()
                    .withName("path1")
                    .withInner(SchemaAtt.create {
                        withName("INNER")
                        withInner(SchemaAtt.create().withName("?disp").build())
                    })
                ).build()
        )

        assertAtt("name?str", "name{?str}")
        assertAtt("name.title?str", "name{title{?str}}")
        assertAtt("name", "name?disp")
    }

    @Test
    fun attTest() {
        assertAtt("cm:na\\.me", "cm:na\\.me")
        assertAtt("?str", "?str")
        assertAtt("cm:title.cm:name?str", "cm:title.cm:name?str")
        assertAtt("cm:name?str", "cm:name?str")
        assertAtt("cm\\:title.cm:name.deep.field", "cm\\:title.cm:name.deep.field?disp")
        assertAtt("cm\\:name", "cm\\:name")
        assertAtt("cm:name.cm:title", "cm:name.cm:title")
        assertAtt("cm:name{al:title}", "cm:name{al:title}")
        assertAtt("cm:name[]{al:title}", "cm:name[]{al:title}")
        assertAtt("cm:name[]{al:title[]}", "cm:name[]{al:title[]}")
        assertAtt("cm:name{al:title[]}", "cm:name{al:title[]}")
        assertAtt("cm:name[].cm:title", "cm:name[].cm:title")
        assertAtt("cm:name[].cm:title[]", "cm:name[].cm:title[]")
        assertAtt("cm:name.cm:title[]", "cm:name.cm:title[]")
        assertAtt("cm:name{.disp:?disp,.str:?str}", "cm:name{.disp,.str}")
        assertAtt("cm:name{disp:?disp,.str:?str}", "cm:name{disp:.disp,.str}")
        assertAtt("cm:name{name,displayName}", "cm:name{name,displayName}")
        assertAtt("cm:name[]{cm\\:name,cm\\:title}", "cm:name[]{cm\\:name,cm\\:title}")
        assertAtt("cm:name", "cm:name")
        assertAtt("cm:na.me", "cm:na.me")
        assertAtt("cm:name[]", "cm:name[]")
        assertAtt("cm:name[]?str", "cm:name[]?str")
        assertAtt("cm:name{strange alias name:?disp,.str:?str}", "cm:name{strange alias name:.disp,.str}")
    }

    @Test
    fun testInnerAliases() {
        assertAtt(
            "cm:name{.disp:?disp,.str:?str}",
            "cm:name{.disp,.str}"
        )
    }

    @Test
    fun testWithError() {
        doWithCtx<Any?>(factory) { ctx: RequestContext ->
            val atts: MutableMap<String, String> = LinkedHashMap()
            atts["abc"] = "def"
            atts["incorrectScalar"] = "def?protected"
            atts["incorrectBrace"] = "def{one"
            atts["incorrectBrace2"] = "def{one{two{}}"
            atts["incorrectBrace3"] = "def{one{two{[}]}}"
            val readedAtts = reader.read(atts)
            assertEquals(4, ctx.getErrors().size)
            assertEquals("abc", readedAtts[0].alias)
            assertEquals("def", readedAtts[0].name)
            assertEquals("incorrectScalar", readedAtts[1].alias)
            assertEquals("_null", readedAtts[1].name)
            assertEquals("incorrectBrace", readedAtts[2].alias)
            assertEquals("_null", readedAtts[2].name)
            assertEquals("incorrectBrace2", readedAtts[3].alias)
            assertEquals("_null", readedAtts[3].name)
            assertEquals("incorrectBrace3", readedAtts[4].alias)
            assertEquals("_null", readedAtts[4].name)
            null
        }
    }

    private fun assertAtt(expected: String, source: String, expectedSchemaAtt: SchemaAtt? = null) {

        val firstSchemaAtt = reader.read(source)

        if (expectedSchemaAtt != null) {
            assertThat(firstSchemaAtt).isEqualTo(expectedSchemaAtt)
        }

        val schemaAttAfterWrite = writer.write(firstSchemaAtt)
        assertThat(schemaAttAfterWrite).isEqualTo(expected)
        val schemaAttAfterWriteAndRead = reader.read(schemaAttAfterWrite)
        assertThat(schemaAttAfterWriteAndRead).isEqualTo(firstSchemaAtt)
        assertThat(writer.write(schemaAttAfterWriteAndRead)).isEqualTo(expected)
    }
}
