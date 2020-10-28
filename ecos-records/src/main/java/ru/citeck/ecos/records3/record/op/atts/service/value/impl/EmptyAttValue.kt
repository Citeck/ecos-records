package ru.citeck.ecos.records3.record.op.atts.service.value.impl

import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue

class EmptyAttValue private constructor() : AttValue {

    companion object {
        val INSTANCE: EmptyAttValue = EmptyAttValue()
    }

    override fun getAtt(name: String): Any? {
        return if (RecordConstants.ATT_NOT_EXISTS == name) {
            true
        } else {
            null
        }
    }

    override fun asText(): String? = null
}
