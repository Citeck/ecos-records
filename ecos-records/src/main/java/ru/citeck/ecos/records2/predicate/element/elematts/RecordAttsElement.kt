package ru.citeck.ecos.records2.predicate.element.elematts

import ru.citeck.ecos.records2.predicate.element.Element
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts

class RecordAttsElement<T : Any>(val obj: T, val atts: RecordAtts) : Element {

    override fun getAttributes(attributes: List<String>): ElementAttributes {
        return RecordAttsElementAtts(atts)
    }
}
