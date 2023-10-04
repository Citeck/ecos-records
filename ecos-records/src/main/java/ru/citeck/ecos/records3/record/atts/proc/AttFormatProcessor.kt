package ru.citeck.ecos.records3.record.atts.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.context.lib.time.TimeZoneContext
import java.sql.Date
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.util.*

class AttFormatProcessor : AbstractAttProcessor<AttFormatProcessor.Args>() {

    companion object {
        private const val DEFAULT_VALUE = "DEFAULT"

        private val DEFAULT_LOCALE = Locale.ENGLISH
        private val UTC_TIMEZONE = TimeZone.getTimeZone("UTC")

        private const val LOCALE_ARG_IDX = 1
        private const val TIMEZONE_ARG_IDX = 2
    }

    override fun processOne(attributes: ObjectData, value: DataValue, args: Args): Any? {

        if (DateTimeAttProcUtils.isDateTimeValue(value)) {

            val dateTime = try {
                ZonedDateTime.parse(DateTimeAttProcUtils.normalizeDateTimeValue(value.asText()))
            } catch (e: DateTimeParseException) {
                return value
            }
            val formatter = SimpleDateFormat(args.format, args.locale)
            formatter.timeZone = args.timeZone ?: if (isFmtContainsTz(args.format)) {
                TimeZone.getTimeZone(dateTime.zone)
            } else {
                val offsetInMinutes = TimeZoneContext.getUtcOffset().toMinutes()
                if (offsetInMinutes == 0L) {
                    UTC_TIMEZONE
                } else {
                    val hours = (offsetInMinutes / 60).toString().padStart(2, '0')
                    val minutes = (offsetInMinutes % 60).toString().padStart(2, '0')
                    TimeZone.getTimeZone("GMT+$hours:$minutes")
                }
            }

            return formatter.format(Date.from(dateTime.toInstant()))
        } else if (value.isNumber() || value.isTextual()) {

            val parsed: Double = value.asDouble(Double.NaN)
            if (!java.lang.Double.isNaN(parsed)) {
                val format = DecimalFormat(
                    args.format,
                    DecimalFormatSymbols.getInstance(args.locale)
                )
                return format.format(parsed)
            }
        }
        return value
    }

    private fun isFmtContainsTz(format: String): Boolean {
        return format.contains("Z") ||
            format.contains("z") ||
            format.contains("X")
    }

    override fun parseArgs(args: List<DataValue>): Args {

        var locale: Locale = DEFAULT_LOCALE

        if (args.size > LOCALE_ARG_IDX) {
            val localeStr: String = args[LOCALE_ARG_IDX].asText()
            if (StringUtils.isNotBlank(localeStr)) {
                locale = if (localeStr == DEFAULT_VALUE) {
                    Locale.getDefault()
                } else {
                    Locale(localeStr)
                }
            }
        }

        var timeZone: TimeZone? = null

        if (args.size > TIMEZONE_ARG_IDX) {
            val timeZoneStr: String = args[TIMEZONE_ARG_IDX].asText()
            if (StringUtils.isNotBlank(timeZoneStr)) {
                timeZone = if (timeZoneStr == DEFAULT_VALUE) {
                    TimeZone.getDefault()
                } else {
                    TimeZone.getTimeZone(timeZoneStr)
                }
            }
        }

        return Args(args[0].asText(), locale, timeZone)
    }

    override fun getType() = "fmt"

    data class Args(
        val format: String,
        val locale: Locale = DEFAULT_LOCALE,
        val timeZone: TimeZone? = null
    )
}
