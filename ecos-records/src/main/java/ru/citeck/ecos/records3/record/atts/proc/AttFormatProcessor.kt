package ru.citeck.ecos.records3.record.atts.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.StringUtils
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

        // Value 50 is not something special, but we can
        // be sure that a date will not be longer than this value
        private const val DATE_MAX_LENGTH = 50

        private const val ZULU_TIME_POSTFIX = "T00:00:00Z"
    }

    override fun processOne(attributes: ObjectData, value: DataValue, args: Args): Any? {

        if (value.isTextual() && isDateValue(value.asText())) {

            val formatter = SimpleDateFormat(args.format, args.locale)

            val dateTime = try {
                ZonedDateTime.parse(normalizeDateTimeValue(value.asText()))
            } catch (e: DateTimeParseException) {
                return value
            }
            formatter.timeZone = args.timeZone ?: if (isFmtContainsTz(args.format)) {
                TimeZone.getTimeZone(dateTime.zone)
            } else {
                UTC_TIMEZONE
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

    private fun normalizeDateTimeValue(value: String): String {
        return if (value.endsWith("Z")) {
            value
        } else if (value.length == 10) {
            // check for yyyy-MM-dd ("2020-01-01") date format
            if (value[4] == '-' && value[7] == '-') {
                value + ZULU_TIME_POSTFIX
                // check for dd-MM-yyyy ("01-01-2020") date format
            } else if (value[2] == '-' && value[5] == '-') {
                val parts = value.split("-")
                if (parts.size != 3) {
                    value
                } else {
                    parts[2] + "-" + parts[1] + "-" + parts[0] + ZULU_TIME_POSTFIX
                }
            } else {
                value
            }
        } else {
            value
        }
    }

    private fun isFmtContainsTz(format: String): Boolean {
        return format.contains("Z") ||
            format.contains("z") ||
            format.contains("X")
    }

    private fun isDateValue(value: String): Boolean {
        return if (value.isBlank() || value.length > DATE_MAX_LENGTH) {
            false
        } else if (value.length > 10) {
            value.contains("T")
        } else {
            return value.indexOf('-') > 0
        }
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
