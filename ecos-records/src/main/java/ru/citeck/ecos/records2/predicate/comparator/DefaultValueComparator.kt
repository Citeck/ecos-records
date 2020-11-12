package ru.citeck.ecos.records2.predicate.comparator

import ru.citeck.ecos.commons.data.DataValue
import java.time.Instant
import kotlin.math.abs

class DefaultValueComparator : ValueComparator {

    companion object {
        private const val DOUBLE_THRESHOLD = 0.00000001
    }

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
        return if (value0.isTextual() || value1.isTextual()) {
            value0.asText() == value1.asText()
        } else {
            false
        }
    }

    override fun isContains(value: DataValue, subValue: DataValue): Boolean {
        if (value.isNull() || subValue.isNull()) {
            return false
        }
        if (value.isArray()) {
            return value.any { it == subValue }
        }
        if (value.isTextual() || subValue.isTextual()) {
            val v0 = value.asText().toLowerCase()
            val v1 = subValue.asText().toLowerCase()
            return v0.contains(v1)
        }
        return false
    }

    override fun isIn(value: DataValue, inValue: DataValue): Boolean {
        if (value.isNull() || inValue.isNull()) {
            return false
        }
        if (inValue.isArray()) {
            return inValue.any { it == value }
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
        val valueStr = value.asText().toLowerCase()
        var likeStr = likeValue.asText().toLowerCase()
        likeStr = likeStr.replace("%", ".*")
        likeStr = likeStr.replace("_", ".")
        return valueStr.matches(likeStr.toRegex())
    }

    override fun isEmpty(value: DataValue): Boolean {
        if (value.isNull()) {
            return true
        }
        if (value.isTextual()) {
            return value.asText().isEmpty()
        }
        return value.size() == 0
    }

    private fun compareGL(
        value0: DataValue,
        value1: DataValue,
        isGreater: Boolean,
        inclusive: Boolean
    ): Boolean {

        if (value0.isNull() || value1.isNull()) {
            return false
        }
        var result = compareDouble(value0, value1, isGreater, inclusive)
        if (result == CompareResult.UNKNOWN) {
            val intRes = value0.asText().compareTo(value1.asText())
            result = if (intRes == 0) {
                if (inclusive) {
                    CompareResult.TRUE
                } else {
                    CompareResult.FALSE
                }
            } else if (intRes == 1) {
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

        if (if (isGreater) doubleValue0 > doubleValue1 else doubleValue0 < doubleValue1) {
            return CompareResult.TRUE
        } else if (inclusive) {
            return if (abs(doubleValue0 - doubleValue1) < DOUBLE_THRESHOLD) {
                CompareResult.TRUE
            } else {
                CompareResult.FALSE
            }
        }
        return CompareResult.FALSE
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
        return value.asDouble(Double.NaN)
    }

    private enum class CompareResult {
        TRUE, FALSE, UNKNOWN
    }
}
