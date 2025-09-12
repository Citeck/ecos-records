package ru.citeck.ecos.records3.test.record.atts.proc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.citeck.ecos.records3.RecordsServiceFactory

class BitsProcessorsTest {

    @ParameterizedTest
    @CsvSource(
        "1, AND, 1, 1",
        "1, AND, 0, 0",
        "1, OR, 1, 1",
        "1, OR, 0, 1",
        "1, SHL, 0, 1",
        "1, SHL, 2, 4",
        "0, SHL, 2, 0",
        "2, SHL, 2, 8",
        "8, SHR, 2, 2",
        "8, SHR, 2, 2",
        "8, USHR, 2, 2",
        "8, IS_SET, 0, 0",
        "8, IS_SET, 1, 0",
        "8, IS_SET, 3, 1",
        "8, IS_SET, 5, 0",
        "514, IS_SET, 1, 1",
        "514, IS_SET, 0, 0",
        "515, IS_SET, 0, 1",
    )
    fun operatorsTest(arg0: Long, operator: Operator, arg1: Long, expected: Long) {
        val records = RecordsServiceFactory().recordsService
        val result = records.getAtt(null, "\$num.$arg0|${operator.procName}($arg1)").asLong()
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun testWithPipe() {
        val records = RecordsServiceFactory().recordsService

        fun process(numAndProcessors: String): Number {
            return records.getAtt(null, "\$num.$numAndProcessors").asJavaObj() as Number
        }

        assertThat(process("123?num")).isEqualTo(123.0)
        assertThat(process("123?num|plus(1)")).isEqualTo(124.0)
        assertThat(process("123?num|plus(0xF)")).isEqualTo(138.0)
        assertThat(process("123|bitAnd(0b11)")).isEqualTo(3L)
        assertThat(process("0xFFF|bitAnd(0x1F)")).isEqualTo(0x1FL)
    }

    @Suppress("unused")
    enum class Operator(
        val procName: String
    ) {
        AND("bitAnd"),
        OR("bitOr"),
        SHL("bitShl"),
        SHR("bitShr"),
        USHR("bitUShr"),
        IS_SET("bitIsSet")
    }
}
