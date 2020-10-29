package ru.citeck.ecos.records3.record.op.atts.service.value.impl

import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue

class RecordAttValue(val atts: RecordAtts) : AttValue {

    override fun getId(): String? {
        return atts.getId().toString()
    }

    override fun asText(): String? {
        return atts.getId().toString()
    }

    override fun getAtt(name: String): Any? {
        return atts.getAtt(name)
    }
}
