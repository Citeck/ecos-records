package ru.citeck.ecos.records3.test.record.atts.schema

import io.github.oshai.kotlinlogging.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.io.file.std.EcosStdFile
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.proc.AttProcDef
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import java.io.File

class SchemaReadWriteTest {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val services = RecordsServiceFactory()

    private val writer = services.attSchemaWriter
    private val reader = services.attSchemaReader

    @Test
    fun testInCode() {

        testAtt(
            "document{" +
                "idocs\\:initiator.em:changeRequestDueDateRequestor," +
                "idocs\\:initiator.cm:userName" +
                "}",
            SchemaAtt.create()
                .withName("document")
                .withInner(
                    listOf(
                        SchemaAtt.create()
                            .withName("idocs:initiator")
                            .withInner(
                                SchemaAtt.create()
                                    .withName("em:changeRequestDueDateRequestor")
                                    .withInner(SchemaAtt.create { withName("?disp") })
                                    .build()
                            ).build(),
                        SchemaAtt.create()
                            .withName("idocs:initiator")
                            .withInner(
                                SchemaAtt.create()
                                    .withName("cm:userName")
                                    .withInner(SchemaAtt.create { withName("?disp") })
                                    .build()
                            ).build()
                    )
                )
                .build()
        )

        testAtt(
            "document{" +
                "a:idocs:initiator.em:changeRequestDueDateRequestor," +
                "b:idocs:initiator.cm:userName" +
                "}",
            SchemaAtt.create()
                .withName("document")
                .withInner(
                    listOf(
                        SchemaAtt.create()
                            .withAlias("a")
                            .withName("idocs:initiator")
                            .withInner(
                                SchemaAtt.create()
                                    .withName("em:changeRequestDueDateRequestor")
                                    .withInner(SchemaAtt.create { withName("?disp") })
                                    .build()
                            ).build(),
                        SchemaAtt.create()
                            .withAlias("b")
                            .withName("idocs:initiator")
                            .withInner(
                                SchemaAtt.create()
                                    .withName("cm:userName")
                                    .withInner(SchemaAtt.create { withName("?disp") })
                                    .build()
                            ).build()
                    )
                )
                .build()
        )

        testAtt(
            "document{s:idocs:initiator.cm:userName}",
            SchemaAtt.create()
                .withName("document")
                .withInner(
                    SchemaAtt.create()
                        .withAlias("s")
                        .withName("idocs:initiator")
                        .withInner(
                            SchemaAtt.create()
                                .withName("cm:userName")
                                .withInner(SchemaAtt.create().withName("?disp"))
                        )
                ).build()
        )
    }

    @Test
    fun testWithFiles() {

        val root = EcosStdFile(File("./src/test/resources/schema-read-write"))
        val testFiles = root.findFiles("**.json")

        log.info { "Found ${testFiles.size} test files" }

        testFiles.forEach { fileWithTests ->

            log.info { "Test: ${fileWithTests.getPath()}" }
            val tests = Json.mapper.read(fileWithTests, TestsDto::class.java) ?: error("File reading failed")
            log.info { "Tests count: ${tests.tests.size}" }

            var idx = 0
            for (test in tests.tests) {
                try {
                    testAtt(test.att, test.exp, test.expAfterWrite)
                    idx++
                } catch (e: Throwable) {
                    log.error { "$idx att: ${test.att}" }
                    throw e
                }
            }

            val fullAtt = SchemaAtt.create()
                .withName("fileAll")
                .withInner(tests.tests.map { it.exp })
                .build()

            testAtt(writer.write(fullAtt), fullAtt)
        }
    }

    @Test
    fun testScalarWithProc() {

        val def = SchemaAtt.create()
            .withName("field")
            .withInner(
                listOf(
                    SchemaAtt.create()
                        .withName("?str")
                        .withProcessors(listOf(AttProcDef("presuf", listOf(DataValue.createStr("arg")))))
                        .build()
                )
            ).build()

        assertThat(writer.write(def)).isEqualTo("field{?str|presuf('arg')}")

        val outerDef = SchemaAtt.create()
            .withName("outerField")
            .withInner(def)
            .build()

        assertThat(writer.write(outerDef)).isEqualTo("outerField.field{?str|presuf('arg')}")

        val outerDefWithMultipleInnerAtts = SchemaAtt.create()
            .withName("outerField")
            .withInner(listOf(def, def.copy { withName("field1") }))
            .build()

        assertThat(writer.write(outerDefWithMultipleInnerAtts))
            .isEqualTo("outerField{field{?str|presuf('arg')},field1{?str|presuf('arg')}}")
    }

    private fun testAtt(att: String, expected: SchemaAtt?, expAfterWrite: String = "") {

        val parsedAtt = reader.read(att)
        if (expected != null) {
            assertThat(parsedAtt).describedAs(att).isEqualTo(expected)
        }

        val attAfterWrite = writer.write(parsedAtt)
        if (expAfterWrite.isBlank()) {
            assertThat(attAfterWrite).isEqualTo(att)
        } else {
            assertThat(attAfterWrite).isEqualTo(expAfterWrite)
            testAtt(attAfterWrite, expected)
        }
    }

    data class TestsDto(
        val tests: List<TestDto>
    )

    data class TestDto(
        val att: String,
        val exp: SchemaAtt,
        val expAfterWrite: String = ""
    )
}
