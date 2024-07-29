package ru.citeck.ecos.records3.test.record.atts.value.factory

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ru.citeck.ecos.records3.RecordsServiceFactory
import java.time.OffsetDateTime
import java.time.ZonedDateTime

class OffsetDateTimeTest {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "2022-01-01T00:00:00+07:00",
            "2022-01-01T00:00:00+01:00",
            "2022-01-01T00:00:01+03:00",
            "2022-01-11T11:11:11+03:00",
            "2022-01-11T11:11:11Z",
        ]
    )
    fun test(valueStr: String) {

        val records = RecordsServiceFactory().recordsService
        val expectedDateTime = OffsetDateTime.parse(valueStr)
        val value = Value(expectedDateTime)

        val fromAttStr = records.getAtt(value, "field").asText()
        assertThat(OffsetDateTime.parse(fromAttStr)).isEqualTo(expectedDateTime)
        assertThat(ZonedDateTime.parse(fromAttStr)).isEqualTo(expectedDateTime.toZonedDateTime())

        // todo: incorrect conversion of ISO8601 string with offset to OffsetDateTime
        // val dtoFromAtts = records.getAtts(value, Value::class.java)
        // assertThat(dtoFromAtts).isEqualTo(value)
    }

    data class Value(
        val field: OffsetDateTime
    )
}
