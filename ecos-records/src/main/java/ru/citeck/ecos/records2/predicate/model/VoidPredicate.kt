package ru.citeck.ecos.records2.predicate.model

import com.fasterxml.jackson.annotation.JsonValue

/**
 * This predicate used when predicate is null or not-parsable value.
 * For conditions this predicate should be interpreted as AlwaysTruePredicate
 */
class VoidPredicate private constructor() : Predicate {

    companion object {
        @JvmField
        val INSTANCE = VoidPredicate()
    }

    @JsonValue
    private fun jsonValue(): Any {
        return emptyMap<Any, Any>()
    }

    override fun <T : Predicate> copy(): T {
        @Suppress("UNCHECKED_CAST")
        return INSTANCE as T
    }

    override fun toString(): String {
        return "{}"
    }

    override fun hashCode(): Int {
        return VoidPredicate::class.java.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is VoidPredicate
    }
}
