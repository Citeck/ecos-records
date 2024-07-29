package ru.citeck.ecos.records2.predicate.model

import com.fasterxml.jackson.annotation.JsonProperty

class NotPredicate : Predicate {

    companion object {

        @JsonProperty("t")
        const val TYPE = "not"

        @JvmStatic
        fun getTypes(): List<String> {
            return listOf(TYPE)
        }
    }

    @JsonProperty("val")
    private var predicate: Predicate = VoidPredicate.INSTANCE

    constructor()

    constructor(predicate: Predicate?) {
        this.predicate = predicate ?: VoidPredicate.INSTANCE
    }

    @JsonProperty("t")
    fun getType(): String {
        return TYPE
    }

    fun getPredicate(): Predicate {
        return predicate
    }

    fun setPredicate(predicate: Predicate) {
        this.predicate = predicate
    }

    @JsonProperty("v")
    fun setVal(predicate: Predicate) {
        setPredicate(predicate)
    }

    override fun <T : Predicate> copy(): T {
        @Suppress("UNCHECKED_CAST")
        return NotPredicate(predicate.copy()) as T
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as NotPredicate
        return predicate == that.predicate
    }

    override fun hashCode(): Int {
        return predicate.hashCode()
    }

    override fun toString(): String {
        return "{\"t\":\"not\",\"val\":$predicate}"
    }
}
