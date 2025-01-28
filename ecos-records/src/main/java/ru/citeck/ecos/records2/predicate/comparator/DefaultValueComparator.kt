package ru.citeck.ecos.records2.predicate.comparator

import ru.citeck.ecos.commons.data.DataValue
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlin.math.abs

object DefaultValueComparator : ValueComparator {

    private const val DOUBLE_THRESHOLD = 0.0000001

    operator fun invoke(): DefaultValueComparator = this

    override fun isEquals(value0: DataValue, value1: DataValue): Boolean {
        if (value0 === value1) {
            return true
        }
        if (value0.isNull() || value1.isNull()) {
            return false
        }
        if (value0.isNumber() && value1.isNumber()) {
            val v0: Double = value0.doubleValue()
            val v1: Double = value1.doubleValue()
            return abs(v0 - v1) < DOUBLE_THRESHOLD
        }
        when (compareDateTime(value0, value1, CompareType.EQUALS, true)) {
            CompareResult.TRUE -> return true
            CompareResult.FALSE -> return false
            CompareResult.UNKNOWN -> {}
        }
        if (value0.isTextual() && value1.isNumber() || value0.isNumber() && value1.isTextual()) {
            val v0 = toDouble(value0)
            val v1 = toDouble(value1)
            if (!v0.isNaN() && !v1.isNaN()) {
                return abs(v0 - v1) < DOUBLE_THRESHOLD
            }
        }
        if (value0.isObject() && value1.isTextual()) {
            val val1Str = value1.asText()
            return compareObjStrValues(value0) { it == val1Str }
        }
        if (value0.isTextual() || value1.isTextual()) {
            return value0.asText() == value1.asText()
        }
        return value0 == value1
    }

    override fun isContains(value: DataValue, subValue: DataValue): Boolean {
        if (value.isNull() || subValue.isNull()) {
            return false
        }
        if (value.isArray()) {
            return if (subValue.isArray()) {
                value.any { v ->
                    subValue.any { sv ->
                        isEquals(v, sv)
                    }
                }
            } else {
                value.any { isEquals(it, subValue) }
            }
        }
        if (value.isObject() && subValue.isTextual()) {
            val subValStr = subValue.asText().lowercase()
            return compareObjStrValues(value) { it.lowercase().contains(subValStr) }
        }
        if (value.isTextual() || subValue.isTextual()) {
            val v0 = value.asText().lowercase()
            val v1 = subValue.asText().lowercase()
            return v0.contains(v1)
        }
        return false
    }

    override fun isIn(value: DataValue, inValue: DataValue): Boolean {
        if (value.isNull() || inValue.isNull()) {
            return false
        }
        if (inValue.isArray()) {
            return inValue.any { isEquals(it, value) }
        }
        return false
    }

    override fun isGreaterThan(value: DataValue, thanValue: DataValue, inclusive: Boolean): Boolean {
        return compareGL(value, thanValue, true, inclusive)
    }

    override fun isLessThan(value: DataValue, thanValue: DataValue, inclusive: Boolean): Boolean {
        return compareGL(value, thanValue, false, inclusive)
    }

    override fun isLike(value: DataValue, likeValue: DataValue): Boolean {
        if (value.isNull() || likeValue.isNull()) {
            return false
        }
        var likeStr = likeValue.asText().lowercase()
        likeStr = likeStr.replace("%", ".*")
        likeStr = likeStr.replace("_", ".")
        val regex = likeStr.toRegex()

        if (value.isObject()) {
            return compareObjStrValues(value) { it.lowercase().matches(regex) }
        }
        val valueStr = value.asText().lowercase()
        return valueStr.matches(regex)
    }

    override fun isEmpty(value: DataValue): Boolean {
        if (value.isNull()) {
            return true
        }
        return value.isEmpty()
    }

    private fun compareGL(
        value0: DataValue,
        value1: DataValue,
        isGreater: Boolean,
        inclusive: Boolean
    ): Boolean {

        if (value0.isNull() && value1.isNull()) {
            return inclusive
        }
        val compareType = if (isGreater) { CompareType.GREATER } else { CompareType.LESS }
        var result = compareDateTime(value0, value1, compareType, inclusive)
        if (result == CompareResult.UNKNOWN) {
            result = compareDouble(value0, value1, isGreater, inclusive)
        }
        if (result == CompareResult.UNKNOWN) {
            val intRes = value0.asText().compareTo(value1.asText())
            result = if (intRes == 0) {
                if (inclusive) {
                    CompareResult.TRUE
                } else {
                    CompareResult.FALSE
                }
            } else if (intRes > 0) {
                if (isGreater) {
                    CompareResult.TRUE
                } else {
                    CompareResult.FALSE
                }
            } else {
                if (isGreater) {
                    CompareResult.FALSE
                } else {
                    CompareResult.TRUE
                }
            }
        }
        return result == CompareResult.TRUE
    }

    private fun compareDateTime(
        value0: DataValue,
        value1: DataValue,
        type: CompareType,
        inclusive: Boolean
    ): CompareResult {

        val date0 = parseDateTime(value0) ?: return CompareResult.UNKNOWN
        val date1 = parseDateTime(value1) ?: return CompareResult.UNKNOWN

        val compareRes = date0.compareTo(date1)

        if (inclusive && compareRes == 0) {
            return CompareResult.TRUE
        }
        val boolRes = when (type) {
            CompareType.EQUALS -> compareRes == 0
            CompareType.GREATER -> compareRes > 0
            CompareType.LESS -> compareRes < 0
        }
        return if (boolRes) {
            CompareResult.TRUE
        } else {
            CompareResult.FALSE
        }
    }

    private fun parseDateTime(value: DataValue): Instant? {
        if (!value.isTextual()) {
            return null
        }
        val txt = value.asText()
        if (!txt.contains('T') || !txt.endsWith('Z')) {
            return null
        }
        return try {
            Instant.parse(txt)
        } catch (e: DateTimeParseException) {
            return null
        }
    }

    private fun compareDouble(
        value0: DataValue,
        value1: DataValue,
        isGreater: Boolean,
        inclusive: Boolean
    ): CompareResult {

        val doubleValue0 = toDouble(value0)
        val doubleValue1 = toDouble(value1)

        if (doubleValue0.isNaN() || doubleValue1.isNaN()) {
            return CompareResult.UNKNOWN
        }

        return if (abs(doubleValue0 - doubleValue1) < DOUBLE_THRESHOLD) {
            if (inclusive) {
                CompareResult.TRUE
            } else {
                CompareResult.FALSE
            }
        } else {
            if (isGreater) {
                if (doubleValue0 > doubleValue1) {
                    CompareResult.TRUE
                } else {
                    CompareResult.FALSE
                }
            } else {
                if (doubleValue0 < doubleValue1) {
                    CompareResult.TRUE
                } else {
                    CompareResult.FALSE
                }
            }
        }
    }

    private fun toDouble(value: DataValue): Double {

        if (value.isTextual()) {
            val text = value.asText()
            if (text.contains("T") && text.endsWith("Z")) {
                return try {
                    Instant.parse(text).toEpochMilli().toDouble()
                } catch (e: Exception) {
                    Double.NaN
                }
            }
        }
        if (value.isNull()) {
            return Double.MIN_VALUE
        }
        return value.asDouble(Double.NaN)
    }

    private inline fun compareObjStrValues(obj: DataValue, compare: (String) -> Boolean): Boolean {
        val it = obj.fieldNames()
        while (it.hasNext()) {
            val value = obj[it.next()]
            if (value.isTextual() && compare(value.asText())) {
                return true
            }
        }
        return false
    }

    private enum class CompareResult {
        TRUE, FALSE, UNKNOWN
    }

    private enum class CompareType {
        EQUALS, GREATER, LESS
    }
}
