package ru.citeck.ecos.records2.predicate.model

import java.time.Instant
import java.time.OffsetDateTime
import java.util.*

object Predicates {

    @JvmStatic
    fun alwaysTrue(): Predicate {
        return VoidPredicate.INSTANCE
    }

    @JvmStatic
    fun alwaysFalse(): Predicate {
        return not(alwaysTrue())
    }

    @JvmStatic
    fun inVals(attribute: String?, values: Collection<String>): ValuePredicate {
        return `in`(attribute, values)
    }

    @JvmStatic
    fun `in`(attribute: String?, values: Collection<String>): ValuePredicate {
        return ValuePredicate(attribute, ValuePredicate.Type.IN, ArrayList(values))
    }

    @JvmStatic
    fun contains(attribute: String?, substring: String?): ValuePredicate {
        return ValuePredicate.contains(attribute, substring)
    }

    @JvmStatic
    fun and(vararg predicates: Predicate?): AndPredicate {
        return AndPredicate.of(predicates.filterNotNull())
    }

    @JvmStatic
    fun and(predicates: Collection<Predicate?>): AndPredicate {
        val and = AndPredicate()
        and.setPredicates(predicates.filterNotNull())
        return and
    }

    @JvmStatic
    fun or(predicates: Collection<Predicate?>): OrPredicate {
        val or = OrPredicate()
        or.setPredicates(predicates.filterNotNull())
        return or
    }

    @JvmStatic
    fun or(vararg predicates: Predicate?): OrPredicate {
        return OrPredicate.of(predicates.filterNotNull())
    }

    @JvmStatic
    fun not(predicate: Predicate?): NotPredicate {
        return NotPredicate(predicate)
    }

    @JvmStatic
    fun notEmpty(attribute: String?): NotPredicate {
        return not(empty(attribute))
    }

    @JvmStatic
    fun notEq(attribute: String?, value: Any?): NotPredicate {
        return not(eq(attribute, value))
    }

    @JvmStatic
    fun notContains(attribute: String?, value: String?): NotPredicate {
        return not(contains(attribute, value))
    }

    @JvmStatic
    fun empty(attribute: String?): EmptyPredicate {
        return EmptyPredicate(attribute)
    }

    @JvmStatic
    fun like(attribute: String?, value: Any?): ValuePredicate {
        return ValuePredicate.like(attribute, value)
    }

    @JvmStatic
    fun eq(attribute: String?, value: Any?): ValuePredicate {
        return equal(attribute, value)
    }

    @JvmStatic
    fun equal(attribute: String?, value: Any?): ValuePredicate {
        return ValuePredicate.equal(attribute, value)
    }

    @JvmStatic
    fun gt(attribute: String?, value: Instant?): ValuePredicate {
        return ValuePredicate.gt(attribute, value)
    }

    @JvmStatic
    fun gt(attribute: String?, value: OffsetDateTime?): ValuePredicate {
        return ValuePredicate.gt(attribute, value)
    }

    @JvmStatic
    fun gt(attribute: String?, value: Date?): ValuePredicate {
        return ValuePredicate.gt(attribute, value)
    }

    @JvmStatic
    fun gt(attribute: String?, value: Double): ValuePredicate {
        return ValuePredicate.gt(attribute, value)
    }

    @JvmStatic
    fun gt(attribute: String?, value: Number): ValuePredicate {
        return ValuePredicate.gt(attribute, value)
    }

    @JvmStatic
    fun ge(attribute: String?, value: OffsetDateTime?): ValuePredicate {
        return ValuePredicate.ge(attribute, value)
    }

    @JvmStatic
    fun ge(attribute: String?, value: Instant?): ValuePredicate {
        return ValuePredicate.ge(attribute, value)
    }

    @JvmStatic
    fun ge(attribute: String?, value: Date?): ValuePredicate {
        return ValuePredicate.ge(attribute, value)
    }

    @JvmStatic
    fun ge(attribute: String?, value: Double): ValuePredicate {
        return ValuePredicate.ge(attribute, value)
    }

    @JvmStatic
    fun ge(attribute: String?, value: Number): ValuePredicate {
        return ValuePredicate.ge(attribute, value)
    }

    @JvmStatic
    fun lt(attribute: String?, value: OffsetDateTime?): ValuePredicate {
        return ValuePredicate.lt(attribute, value)
    }

    @JvmStatic
    fun lt(attribute: String?, value: Instant?): ValuePredicate {
        return ValuePredicate.lt(attribute, value)
    }

    @JvmStatic
    fun lt(attribute: String?, value: Date?): ValuePredicate {
        return ValuePredicate.lt(attribute, value)
    }

    @JvmStatic
    fun lt(attribute: String?, value: Double): ValuePredicate {
        return ValuePredicate.lt(attribute, value)
    }

    @JvmStatic
    fun lt(attribute: String?, value: Number): ValuePredicate {
        return ValuePredicate.lt(attribute, value)
    }

    @JvmStatic
    fun le(attribute: String?, value: Instant?): ValuePredicate {
        return ValuePredicate.le(attribute, value)
    }

    @JvmStatic
    fun le(attribute: String?, value: OffsetDateTime?): ValuePredicate {
        return ValuePredicate.le(attribute, value)
    }

    @JvmStatic
    fun le(attribute: String?, value: Date?): ValuePredicate {
        return ValuePredicate.le(attribute, value)
    }

    @JvmStatic
    fun le(attribute: String?, value: Double): ValuePredicate {
        return ValuePredicate.le(attribute, value)
    }

    @JvmStatic
    fun le(attribute: String?, value: Number): ValuePredicate {
        return ValuePredicate.le(attribute, value)
    }
}
