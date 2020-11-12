package ru.citeck.ecos.records2.predicate.comparator

import ru.citeck.ecos.commons.data.DataValue

interface ValueComparator {

    fun isEquals(value0: DataValue, value1: DataValue): Boolean

    fun isContains(value: DataValue, subValue: DataValue): Boolean

    fun isIn(value: DataValue, inValue: DataValue): Boolean

    fun isGreaterThan(value: DataValue, thanValue: DataValue, inclusive: Boolean): Boolean

    fun isLessThan(value: DataValue, thanValue: DataValue, inclusive: Boolean): Boolean

    fun isLike(value: DataValue, likeValue: DataValue): Boolean

    fun isEmpty(value: DataValue): Boolean
}
