package ru.citeck.ecos.records2.predicate.model

import ecos.com.fasterxml.jackson210.annotation.JsonProperty
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

    fun addPredicate(predicate: Predicate) {
        if (predicates == null) {
            predicates = ArrayList()
        }
        predicates!!.add(predicate)
    }
}
