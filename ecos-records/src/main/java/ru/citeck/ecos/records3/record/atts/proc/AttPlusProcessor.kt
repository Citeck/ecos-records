package ru.citeck.ecos.records3.record.atts.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

class AttPlusProcessor : AbstractAttProcessor<AttPlusProcessor.Args>() {

    override fun processOne(attributes: ObjectData, value: DataValue, args: Args): Any? {

        if (args.value.isNull()) {
            return value
        }

        if (DateTimeAttProcUtils.isDateTimeValue(value)) {

            if (!args.value.isTextual()) {
                return value
            }
            val durationStr = args.value.asText()
            if (!durationStr.startsWith("P") && !durationStr.startsWith("-P")) {
                return value
            }
            val dateTime = try {
                ZonedDateTime.parse(DateTimeAttProcUtils.normalizeDateTimeValue(value.asText()))
            } catch (e: DateTimeParseException) {
                return value
            }
            return dateTime.plus(Duration.parse(durationStr))
        } else if (value.isNumber() || value.isTextual()) {

            val parsed: Double = value.asDouble(Double.NaN)
            if (!parsed.isNaN()) {
                val numToAdd = args.value.asDouble(Double.NaN)
                if (!numToAdd.isNaN()) {
                    return parsed + numToAdd
                }
            }
        }
        return value
    }

    override fun parseArgs(args: List<DataValue>): Args {
        if (args.isEmpty()) {
            return Args(DataValue.NULL)
        }
        return Args(args[0])
    }

    override fun getType() = "plus"

    data class Args(
        val value: DataValue
    )
}
