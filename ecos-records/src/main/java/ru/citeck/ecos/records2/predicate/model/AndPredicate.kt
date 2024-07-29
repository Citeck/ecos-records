package ru.citeck.ecos.records2.predicate.model

import com.fasterxml.jackson.annotation.JsonProperty

class AndPredicate : ComposedPredicate() {

    companion object {

        const val TYPE = "and"

        @JvmStatic
        fun getTypes(): List<String> {
            return listOf(TYPE)
        }

        @JvmStatic
        fun of(vararg predicates: Predicate): AndPredicate {
            return of(listOf(*predicates))
        }

        @JvmStatic
        fun of(predicates: List<Predicate>): AndPredicate {
            val and = AndPredicate()
            and.setPredicates(predicates)
            return and
        }
    }

    @JsonProperty("t")
    fun getType(): String {
        return TYPE
    }

    override fun <T : Predicate> copy(): T {
        val copy = AndPredicate()
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
        if (other == null || AndPredicate::class.java != other::class.java) {
            return false
        }
        other as AndPredicate
        return getPredicates() == other.getPredicates()
    }

    override fun hashCode(): Int {
        return getPredicates().hashCode()
    }
}
