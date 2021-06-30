package ru.citeck.ecos.records3.test.record.atts.schema

import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.io.file.std.EcosStdFile
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.RecordsServiceFactory
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

        testFiles.forEach {

            log.info { "Test: ${it.getPath()}" }
            val tests = Json.mapper.read(it, TestsDto::class.java) ?: error("File reading failed")
            log.info { "Tests count: ${tests.tests.size}" }

            var idx = 0
            for (test in tests.tests) {
                try {
                    testAtt(test.att, test.exp)
                    idx++
                } catch (e: Throwable) {
                    log.error { "$idx att: ${test.att}" }
                    throw e
                }
            }
        }
    }

    private fun testAtt(att: String, expected: SchemaAtt?) {

        val parsedAtt = reader.read(att)
        if (expected != null) {
            assertThat(parsedAtt).isEqualTo(expected)
        }

        val attAfterWrite = writer.write(parsedAtt)
        assertThat(attAfterWrite).isEqualTo(att)
    }

    data class TestsDto(
        val tests: List<TestDto>
    )

    data class TestDto(
        val att: String,
        val exp: SchemaAtt
    )
}
