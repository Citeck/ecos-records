package ru.citeck.ecos.records3.record.atts.schema.read

import ru.citeck.ecos.commons.data.DataValue

object ParseUtils {

    fun isNumValue(str: String): Boolean {
        if (str.isEmpty()) {
            return false
        }
        val firstChar = str[0]
        if (firstChar.isDigit()) {
            return true
        }
        if ((firstChar == '-' || firstChar == '+') && str.length > 1) {
            return true
        }
        return false
    }

    fun parseNumValue(str: String): DataValue {
        return DataValue.createAsIs(toRawNumValue(str))
    }

    private fun toRawNumValue(value: String): Number {
        if (value.isEmpty()) {
            return 0L
        }
        var currentIdx = 0
        var negative = false
        if (value[0] == '-') {
            negative = true
            currentIdx++
        } else if (value[0] == '+') {
            currentIdx++
        }
        if (value[currentIdx] == '0') {
            if (++currentIdx >= value.length) {
                return 0L
            }
            var radix = -1
            if (value[currentIdx] == 'b') {
                radix = 2
                currentIdx++
            } else if (value[currentIdx] == 'x') {
                radix = 16
                currentIdx++
            }
            if (radix != -1) {
                if (currentIdx >= value.length) {
                    return 0L
                }
                var res = value.substring(currentIdx).toLong(radix)
                if (negative && res != 0L) {
                    res = res.inv()
                }
                return res
            }
        }
        return if (value.contains('.')) {
            value.toDouble()
        } else {
            value.toLong()
        }
    }
}
