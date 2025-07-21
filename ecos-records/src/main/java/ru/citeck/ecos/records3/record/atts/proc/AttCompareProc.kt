package ru.citeck.ecos.records3.record.atts.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.predicate.comparator.DefaultValueComparator
import ru.citeck.ecos.records2.predicate.comparator.ValueComparator

class AttCompareProc private constructor(
    private val type: CompareType
) : AbstractAttProcessor<AttCompareProc.CompareTo>(true) {

    companion object {
        fun getProcessors(): List<AttCompareProc> {
            return CompareType.entries.map { AttCompareProc(it) }
        }
    }

    private val name = type.name.lowercase()

    private val comparator: ValueComparator = DefaultValueComparator()

    override fun processOne(attributes: ObjectData, value: DataValue, args: CompareTo): Any {
        if (args !is CompareToValue) {
            return false
        }
        return when (type) {
            CompareType.EQ -> comparator.isEquals(value, args.value)
            CompareType.GT -> comparator.isGreaterThan(value, args.value, false)
            CompareType.GE -> comparator.isGreaterThan(value, args.value, true)
            CompareType.LT -> comparator.isLessThan(value, args.value, false)
            CompareType.LE -> comparator.isLessThan(value, args.value, true)
            CompareType.LIKE -> comparator.isLike(value, args.value)
            CompareType.IN -> comparator.isIn(value, args.value)
            CompareType.CONTAINS -> comparator.isContains(value, args.value)
        }
    }

    override fun parseArgs(args: List<DataValue>): CompareTo {
        val firstArg = args.firstOrNull() ?: return CompareToNoValue
        return CompareToValue(firstArg)
    }

    override fun getType(): String {
        return name
    }

    sealed class CompareTo

    data object CompareToNoValue : CompareTo()

    data class CompareToValue(val value: DataValue) : CompareTo()

    enum class CompareType {
        EQ,
        GT,
        GE,
        LT,
        LE,
        LIKE,
        IN,
        CONTAINS
    }
}
