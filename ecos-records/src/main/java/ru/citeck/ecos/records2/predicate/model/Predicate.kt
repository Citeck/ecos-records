package ru.citeck.ecos.records2.predicate.model

interface Predicate {

    fun <T : Predicate> copy(): T
}
