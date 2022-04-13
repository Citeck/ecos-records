package ru.citeck.ecos.records3.test.record.atts.proc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory

class YamlProcessorTest {

    @Test
    fun test() {

        val value = DtoValue("abc", 123)
        val records = RecordsServiceFactory().recordsServiceV1

        val yamlText = records.getAtt(value, "?json|yaml()").asText()
        assertThat(yamlText).isEqualTo(
            """
            ---
            field0: abc
            field1: 123

            """.trimIndent()
        )
    }

    data class DtoValue(
        val field0: String,
        val field1: Int
    )
}
