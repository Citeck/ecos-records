package ru.citeck.ecos.records3.test.record.atts.schema

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt

class SchemaReadWriteTest {

    private val services = RecordsServiceFactory()

    private val writer = services.attSchemaWriter
    private val reader = services.attSchemaReader

    @Test
    fun test() {

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
    }

    fun testAtt(att: String, expected: SchemaAtt?) {

        val parsedAtt = reader.read(att)
        if (expected != null) {
            assertThat(parsedAtt).isEqualTo(expected)
        }

        val attAfterWrite = writer.write(parsedAtt)
        assertThat(attAfterWrite).isEqualTo(att)
    }
}
