package ru.citeck.ecos.records3.test.record.atts.schema

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.write.AttSchemaLegacyWriter

class SchemaLegacyWriteTest {

    @ParameterizedTest
    @CsvSource(
        "field0?str, field0?str",
        "'field0{?str,?bool}', 'field0{.str,.bool}'",
        "field0.field1?str, field0.field1?str",
        "_has.att?bool, .has(n:\"att\")",
        "_edge.att.title?str, .edge(n:\"att\"){title}",
    )
    fun test(source: String, expected: String) {
        val services = RecordsServiceFactory()
        val src = services.attSchemaReader.read(source)
        val out = AttSchemaLegacyWriter().write(src)
        assertThat(out).isEqualTo(expected)
    }

    @Test
    fun scalarsTest() {

        val legacyWriter = AttSchemaLegacyWriter()

        ScalarType.values().forEach {
            val scalarAtt = legacyWriter.write(
                SchemaAtt.create {
                    withName(it.schema)
                }
            )
            val schema = if (it == ScalarType.RAW) {
                "?str"
            } else {
                it.schema
            }
            assertThat(scalarAtt).isEqualTo("." + schema.substring(1))
        }
    }
}
