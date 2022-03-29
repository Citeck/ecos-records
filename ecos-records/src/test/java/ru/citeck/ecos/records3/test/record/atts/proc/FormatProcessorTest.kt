package ru.citeck.ecos.records3.test.record.atts.proc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.atts.proc.AttFormatProcessor

class FormatProcessorTest {

    @Test
    fun test() {

        val formatProc = AttFormatProcessor()
        fun fmt(value: String, format: String, tz: String = "", locale: String = ""): String {
            return Json.mapper.convert(
                formatProc.process(
                    ObjectData.create(),
                    DataValue.createStr(value),
                    listOf(
                        DataValue.createStr(format),
                        DataValue.createStr(locale),
                        DataValue.createStr(tz)
                    )
                ),
                String::class.java
            )!!
        }

        val utcDateTime = "2020-02-01T00:00:00Z"

        assertThat(fmt(utcDateTime, "yyyy")).isEqualTo("2020")
        assertThat(fmt(utcDateTime, "dd-MM-yyyy")).isEqualTo("01-02-2020")

        val moscowDateTime = "2020-02-01T06:00:00+03:00"
        assertThat(fmt(moscowDateTime, "yyyy")).isEqualTo("2020")
        assertThat(fmt(moscowDateTime, "HH")).isEqualTo("03")
        assertThat(fmt(moscowDateTime, "HH X")).isEqualTo("06 +03")
        assertThat(fmt(moscowDateTime, "HH z")).isEqualTo("06 GMT+03:00")
        assertThat(fmt(moscowDateTime, "HH Z")).isEqualTo("06 +0300")
        assertThat(fmt(moscowDateTime, "HH", "GMT+06:00")).isEqualTo("09")
        assertThat(fmt(moscowDateTime, "HH", "GMT+00:00")).isEqualTo("03")

        listOf("2020-02-01", "01-02-2020").forEach {
            assertThat(fmt(it, "yyyy")).describedAs(it).isEqualTo("2020")
            assertThat(fmt(it, "yy.MM.dd")).describedAs(it).isEqualTo("20.02.01")
            assertThat(fmt(it, "HH")).describedAs(it).isEqualTo("00")
        }
    }
}
