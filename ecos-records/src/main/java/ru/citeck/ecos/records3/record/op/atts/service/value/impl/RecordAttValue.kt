package ru.citeck.ecos.records3.record.op.atts.service.value.impl

import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue


class RecordAttValue(atts: RecordAtts?) : AttValue {
    private val atts: RecordAtts?
    override fun getId(): String? {
        return atts.getId().toString()
    }

    override fun asText(): String? {
        return atts.getId().toString()
    }

    override fun getAtt(name: String): Any? {
        return atts.getAtt(name)
    }

    init {
        this.atts = atts
    }
}
