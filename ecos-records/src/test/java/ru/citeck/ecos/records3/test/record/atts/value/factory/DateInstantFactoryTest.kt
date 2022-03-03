package ru.citeck.ecos.records3.test.record.atts.value.factory

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

class DateInstantFactoryTest {

    @Test
    fun test() {

        val instant = Instant.now()
        val expectedYear = instant.atOffset(ZoneOffset.UTC).year

        val records = RecordsServiceFactory().recordsServiceV1

        val testImpl = { time: Any ->
            val year = records.getAtt(time, "?str|fmt('yyyy')").asInt()
            assertThat(year).isEqualTo(expectedYear)
            val ms = records.getAtt(time, "?num").asLong()
            assertThat(ms).isEqualTo(instant.toEpochMilli())
        }

        testImpl(instant)
        testImpl(Date.from(instant))
    }
}
