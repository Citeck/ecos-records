package ru.citeck.ecos.records3.record.atts.value.impl

import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.record.atts.value.AttValue

class EmptyAttValue private constructor() : AttValue {

    companion object {
        @JvmField
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
