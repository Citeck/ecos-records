package ru.citeck.ecos.records2.predicate.model

import com.fasterxml.jackson.annotation.JsonProperty

abstract class AttributePredicate : Predicate {

    @JsonProperty("att")
    private var attribute: String = ""

    @JsonProperty("a")
    fun setAtt(attribute: String?) {
        setAttribute(attribute)
    }

    fun setAttribute(attribute: String?) {
        this.attribute = attribute ?: ""
    }

    fun getAttribute(): String {
        return attribute
    }
}
