package ru.citeck.ecos.records3.record.atts.proc

import ru.citeck.ecos.commons.data.DataValue

object DateTimeAttProcUtils {

    private const val ZULU_TIME_POSTFIX = "T00:00:00Z"

    // Value 50 is not something special, but we can
    // be sure that a date will not be longer than this value
    private const val DATE_MAX_LENGTH = 50

    fun isDateTimeValue(value: DataValue): Boolean {
        if (!value.isTextual()) {
            return false
        }
        val strValue = value.asText()
        return if (strValue.isBlank() || strValue.length > DATE_MAX_LENGTH) {
            false
        } else if (strValue.length > 10) {
            strValue.contains("T")
        } else {
            return strValue.indexOf('-') > 0
        }
    }

    fun normalizeDateTimeValue(value: String): String {
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
}
