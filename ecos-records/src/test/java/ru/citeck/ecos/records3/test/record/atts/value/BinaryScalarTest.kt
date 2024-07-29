package ru.citeck.ecos.records3.test.record.atts.value

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import java.util.*

class BinaryScalarTest {

    @Test
    fun test() {
        val services = RecordsServiceFactory()

        val records = services.recordsService
        val dtoSchemaReader = services.dtoSchemaReader
        val schemaWriter = services.attSchemaWriter

        val att = schemaWriter.write(dtoSchemaReader.read(Value::class.java).first())
        assertThat(att).contains("?bin")

        val srcValue = Value(ByteArray(10) { it.toByte() })
        val targetValue = records.getAtts(srcValue, Value::class.java)

        assertThat(targetValue.array).isEqualTo(srcValue.array)

        val base64ResStr = records.getAtt(srcValue, "array._bin").asText()
        assertThat(Base64.getDecoder().decode(base64ResStr)).isEqualTo(srcValue.array)
    }

    class Value(
        val array: ByteArray
    )
}
