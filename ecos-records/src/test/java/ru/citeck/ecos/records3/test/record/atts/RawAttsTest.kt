package ru.citeck.ecos.records3.test.record.atts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory

class RawAttsTest {

    @Test
    fun notRawTest() {

        val records = RecordsServiceFactory().recordsService
        val testDto = TestDto(inner = TestDto(inner = TestDto()))

        // val res0 = records.getAtt(testDto, "inner.notEmptyStr").asText()
        // assertThat(res0).isEqualTo("not-empty")

        val res1 = records.getAtt(testDto, "inner{emptyStr!'constant'}").asText()
        assertThat(res1).isEqualTo("constant")
    }

    class TestDto(
        val emptyStr: String = "",
        val notEmptyStr: String = "not-empty",
        val inner: TestDto? = null
    )
}
