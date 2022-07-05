package ru.citeck.ecos.records3.record.atts.value.impl

import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.value.AttEdge
import ru.citeck.ecos.records3.record.atts.value.AttValue

class NullAttValue private constructor() : AttValue {

    companion object {
        @JvmField
        val INSTANCE: NullAttValue = NullAttValue()
    }

    override fun getAtt(name: String): Any? {
        return null
    }

    override fun asText(): String? = null

    override fun getId(): Any? {
        return null
    }

    override fun getDisplayName(): Any? {
        return null
    }

    override fun getAs(type: String): Any? {
        return null
    }

    override fun asDouble(): Double? {
        return null
    }

    override fun asBoolean(): Boolean? {
        return null
    }

    override fun asJson(): Any? {
        return null
    }

    override fun asRaw(): Any? {
        return null
    }

    override fun has(name: String): Boolean {
        return false
    }

    override fun getEdge(name: String): AttEdge? {
        return null
    }

    override fun getType(): RecordRef {
        return RecordRef.EMPTY
    }
}
