package ru.citeck.ecos.records2.predicate.model

import ecos.com.fasterxml.jackson210.annotation.JsonProperty

abstract class AttributePredicate : Predicate {

    @JsonProperty("att")
    private var attribute: String = ""

    fun setAttribute(attribute: String?) {
        this.attribute = attribute ?: ""
    }

    fun getAttribute(): String {
        return attribute
    }
}
