package ru.citeck.ecos.records2.predicate.model

import ecos.com.fasterxml.jackson210.annotation.JsonProperty

class OrPredicate : ComposedPredicate() {

    companion object {

        @JsonProperty("t")
        const val TYPE = "or"

        @JvmStatic
        fun getTypes(): List<String> {
            return listOf(TYPE)
        }

        @JvmStatic
        fun of(vararg predicates: Predicate): OrPredicate {
            return of(listOf(*predicates))
        }

        @JvmStatic
        fun of(predicates: List<Predicate>): OrPredicate {
            val or = OrPredicate()
            or.setPredicates(predicates)
            return or
        }
    }

    @JsonProperty("t")
    fun getType(): String {
        return TYPE
    }

    override fun <T : Predicate> copy(): T {
        val copy = OrPredicate()
        copy.setPredicates(getPredicates().map { it.copy<Predicate>() })
        @Suppress("UNCHECKED_CAST")
        return copy as T
    }

    override fun toString(): String {
        return "{\"t\":\"$TYPE\",\"val\":[${getPredicates().joinToString(",") { it.toString() }}]}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || OrPredicate::class.java != other::class.java) {
            return false
        }
        other as OrPredicate
        return getPredicates() == other.getPredicates()
    }

    override fun hashCode(): Int {
        return getPredicates().hashCode()
    }
}
