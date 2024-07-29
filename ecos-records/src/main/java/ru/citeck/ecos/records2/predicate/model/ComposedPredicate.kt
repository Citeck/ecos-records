package ru.citeck.ecos.records2.predicate.model

import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.collections.ArrayList

abstract class ComposedPredicate : Predicate {

    @JsonProperty("val")
    private var predicates: MutableList<Predicate>? = null

    fun getPredicates(): List<Predicate> {
        return predicates ?: arrayListOf()
    }

    fun setPredicates(predicates: List<Predicate>?) {
        this.predicates = ArrayList(predicates ?: emptyList())
    }

    @JsonProperty("v")
    fun setVal(predicates: List<Predicate>?) {
        setPredicates(predicates)
    }

    fun addPredicate(predicate: Predicate) {
        if (predicates == null) {
            predicates = ArrayList()
        }
        predicates!!.add(predicate)
    }
}
