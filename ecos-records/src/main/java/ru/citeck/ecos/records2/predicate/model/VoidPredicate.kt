package ru.citeck.ecos.records2.predicate.model

import ecos.com.fasterxml.jackson210.annotation.JsonValue

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
