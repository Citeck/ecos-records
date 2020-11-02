package ru.citeck.ecos.records3.record.op.atts.service.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.StringUtils
import java.sql.Date
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class AttFormatProcessor : AbstractAttProcessor<AttFormatProcessor.Args>() {

    companion object {
        private val DEFAULT_LOCALE = Locale.ENGLISH
        private val DEFAULT_TIMEZONE = TimeZone.getTimeZone("UTC")
        private const val LOCALE_ARG_IDX = 1
        private const val TIMEZONE_ARG_IDX = 2
    }

    override fun processOne(attributes: ObjectData, value: DataValue, args: Args): Any? {

        if (value.isTextual() && value.asText().endsWith("Z")) {

            val formatter = SimpleDateFormat(args.format, args.locale)
            formatter.timeZone = args.timeZone
            return formatter.format(Date.from(Instant.parse(value.asText())))
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

    override fun parseArgs(args: List<DataValue>): Args {

        var locale: Locale = DEFAULT_LOCALE

        if (args.size > LOCALE_ARG_IDX) {
            val localeStr: String = args[LOCALE_ARG_IDX].asText()
            if (StringUtils.isNotBlank(localeStr)) {
                if ("DEFAULT" == localeStr) {
                    locale = Locale.getDefault()
                } else {
                    locale = Locale(localeStr)
                }
            }
        }

        var timeZone: TimeZone = DEFAULT_TIMEZONE

        if (args.size > TIMEZONE_ARG_IDX) {
            val timeZoneStr: String = args[TIMEZONE_ARG_IDX].asText()
            if (StringUtils.isNotBlank(timeZoneStr)) {
                timeZone = if ("DEFAULT" == timeZoneStr) {
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
        val timeZone: TimeZone = DEFAULT_TIMEZONE
    )
}
