package ru.citeck.ecos.records3.test.record.atts.proc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.time.TimeZoneContext
import ru.citeck.ecos.records3.record.atts.proc.AttFormatProcessor
import java.time.Duration

/**
 * Tests for AttFormatProcessor with negative UTC offsets from TimeZoneContext.
 *
 * Covers a bug where negative offsets produced invalid timezone IDs like "GMT+-5:00"
 * instead of "GMT-05:00". Java's TimeZone.getTimeZone() silently falls back to UTC
 * for unrecognized IDs, causing all dates in negative-offset timezones to be
 * formatted as UTC without any error.
 *
 * The buggy code path is exercised when:
 * 1. No explicit timezone arg is provided (3rd arg is empty)
 * 2. The format string does NOT contain timezone indicators (Z, z, X)
 * 3. TimeZoneContext has a non-zero offset
 * In this case, the processor builds a timezone from TimeZoneContext.getUtcOffset().
 */
class FormatNegativeOffsetTest {

    @Test
    fun negativeOffsetFromContextFormatsCorrectly() {
        val formatProc = AttFormatProcessor()

        // UTC noon
        val utcDateTime = "2020-02-01T12:00:00Z"

        // Set TimeZoneContext to -5 hours (e.g., EST)
        // No explicit timezone arg, format without timezone indicators
        // This exercises the buggy code path: TimeZoneContext.getUtcOffset().toMinutes() = -300
        val result = TimeZoneContext.doWithUtcOffset(Duration.ofHours(-5)) {
            formatProc.process(
                ObjectData.create(),
                DataValue.createStr(utcDateTime),
                listOf(
                    DataValue.createStr("HH"), // no Z/z/X → uses TimeZoneContext offset
                    DataValue.createStr(""), // no locale
                    DataValue.createStr("") // no explicit timezone
                )
            ).toString()
        }

        // UTC 12:00 with offset -5h = 07:00
        // Before fix: "GMT+-5:00" was invalid → Java fell back to UTC → "12"
        assertThat(result).isEqualTo("07")
    }

    @Test
    fun positiveOffsetFromContextFormatsCorrectly() {
        val formatProc = AttFormatProcessor()

        val utcDateTime = "2020-02-01T12:00:00Z"

        val result = TimeZoneContext.doWithUtcOffset(Duration.ofHours(3)) {
            formatProc.process(
                ObjectData.create(),
                DataValue.createStr(utcDateTime),
                listOf(
                    DataValue.createStr("HH"),
                    DataValue.createStr(""),
                    DataValue.createStr("")
                )
            ).toString()
        }

        // UTC 12:00 + 3h = 15:00
        assertThat(result).isEqualTo("15")
    }

    @Test
    fun negativeHalfHourOffsetFromContext() {
        val formatProc = AttFormatProcessor()

        val utcDateTime = "2020-02-01T12:00:00Z"

        val result = TimeZoneContext.doWithUtcOffset(Duration.ofMinutes(-330)) {
            formatProc.process(
                ObjectData.create(),
                DataValue.createStr(utcDateTime),
                listOf(
                    DataValue.createStr("HH:mm"),
                    DataValue.createStr(""),
                    DataValue.createStr("")
                )
            ).toString()
        }

        // UTC 12:00 - 5h30m = 06:30
        assertThat(result).isEqualTo("06:30")
    }
}
