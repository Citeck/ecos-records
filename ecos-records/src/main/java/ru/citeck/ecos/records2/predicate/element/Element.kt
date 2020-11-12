package ru.citeck.ecos.records2.predicate.element

import ru.citeck.ecos.records2.predicate.element.elematts.ElementAttributes

interface Element {
    fun getAttributes(attributes: List<String>): ElementAttributes
}
