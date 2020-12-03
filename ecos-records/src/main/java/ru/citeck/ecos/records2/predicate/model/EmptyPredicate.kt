package ru.citeck.ecos.records2.predicate.model

import ecos.com.fasterxml.jackson210.annotation.JsonProperty

class EmptyPredicate : AttributePredicate {

    companion object {

        const val TYPE = "empty"

        @JvmStatic
        fun getTypes(): List<String> {
            return listOf(TYPE)
        }
    }

    constructor()

    constructor(attribute: String?) {
        setAttribute(attribute)
    }

    @JsonProperty("t")
    fun getType(): String {
        return TYPE
    }

    override fun <T : Predicate> copy(): T {
        @Suppress("UNCHECKED_CAST")
        return EmptyPredicate(getAttribute()) as T
    }

    override fun toString(): String {
        return "{\"t\":\"$TYPE\",\"att\":\"${getAttribute()}\"}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        other as EmptyPredicate
        return getAttribute() == other.getAttribute()
    }

    override fun hashCode(): Int {
        return getAttribute().hashCode()
    }
}
