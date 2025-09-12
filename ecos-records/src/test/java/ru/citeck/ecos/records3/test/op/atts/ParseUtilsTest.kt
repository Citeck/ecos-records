package ru.citeck.ecos.records3.test.op.atts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.citeck.ecos.records3.record.atts.schema.read.ParseUtils

class ParseUtilsTest {

    @ParameterizedTest
    @CsvSource(
        "1, 1",
        "12345, 12345",
        "12345.1, 12345.1",
        "0, 0",
        "0b11, 3",
        "1.2, 1.2",
        "012, 12",
        "0xFF, 255",
    )
    fun parseNumValueTest(toParse: String, expected: String) {
        val expectedNum = if (expected.contains('.')) {
            expected.toDouble()
        } else {
            expected.toLong()
        }
        assertThat(ParseUtils.parseNumValue(toParse).asJavaObj()).isEqualTo(expectedNum)
    }
}
