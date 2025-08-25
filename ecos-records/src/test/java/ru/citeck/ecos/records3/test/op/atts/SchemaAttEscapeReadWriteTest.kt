package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt

class SchemaAttEscapeReadWriteTest {

    @Test
    fun test() {

        val factory = RecordsServiceFactory()
        val writer = factory.attSchemaWriter

        val gqlAtt = """sum(\"AMOUNT\")?str"""
        val srcAtt = """sum(\"AMOUNT\")"""

        assertEquals(
            gqlAtt,
            writer.write(
                SchemaAtt.create {
                    name = srcAtt
                    inner = listOf(SchemaAtt.create { name = "?str" })
                }
            )
        )

        val att = factory.attSchemaReader.read("", gqlAtt)
        assertEquals(srcAtt, att.name)
    }
}
