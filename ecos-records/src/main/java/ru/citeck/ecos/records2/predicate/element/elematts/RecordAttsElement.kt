package ru.citeck.ecos.records2.predicate.element.elematts

import ru.citeck.ecos.records2.predicate.element.Element
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts

class RecordAttsElement<T : Any>(val obj: T, val atts: RecordAtts) : Element {

    companion object {

        @JvmStatic
        fun create(atts: RecordAtts): RecordAttsElement<Unit> {
            return RecordAttsElement(Unit, atts)
        }

        @JvmStatic
        fun <T : Any> create(obj: T, atts: RecordAtts): RecordAttsElement<T> {
            return RecordAttsElement(obj, atts)
        }
    }

    override fun getAttributes(attributes: List<String>): ElementAttributes {
        return RecordAttsElementAtts(atts)
    }
}
