package ru.citeck.ecos.records3.test.record.atts.proc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class AttPlusProcessorTest {

    @Test
    fun instantTest() {

        val instant = Instant.parse("2022-04-28T00:00:00Z")
        val expectedInstantPlus6Hours = instant.plus(Duration.ofHours(6))
        val expectedInstantMinus6Hours = instant.minus(Duration.ofHours(6))

        val records = RecordsServiceFactory().recordsServiceV1
        val instantPlus6Hours = records.getAtt(instant, "_self|plus('PT6H')").getAs(Instant::class.java)!!
        assertThat(instantPlus6Hours).isEqualTo(expectedInstantPlus6Hours)
        val instantMinus6Hours = records.getAtt(instant, "_self|plus('-PT6H')").getAs(Instant::class.java)!!
        assertThat(instantMinus6Hours).isEqualTo(expectedInstantMinus6Hours)

        val atts = records.getAtts(
            "",
            mapOf(
                "nowPlus10h" to "\$now|plus('-PT10H')",
                "now" to "\$now"
            )
        )

        val expected = atts.getAtt("now").getAsInstantOrEpoch()
            .minus(Duration.ofHours(10))
            .truncatedTo(ChronoUnit.HOURS)
        assertThat(
            atts.getAtt("nowPlus10h")
                .getAsInstantOrEpoch()
                .truncatedTo(ChronoUnit.HOURS)
        ).isEqualTo(expected)
    }

    @Test
    fun numTest() {

        val records = RecordsServiceFactory().recordsServiceV1

        val num10 = 10

        val numPlus20 = records.getAtt(num10, "_self|plus(20)").asDouble()
        assertThat(numPlus20).isEqualTo(30.0)

        val numMinus20 = records.getAtt(num10, "_self|plus(-20)").asDouble()
        assertThat(numMinus20).isEqualTo(-10.0)
    }
}
