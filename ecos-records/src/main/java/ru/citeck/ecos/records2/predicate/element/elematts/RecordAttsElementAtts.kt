package ru.citeck.ecos.records2.predicate.element.elematts

import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts

class RecordAttsElementAtts(private val atts: RecordAtts) : ElementAttributes {

    override fun getAttribute(name: String): Any? {
        return atts.getAtt(name)
    }
}
