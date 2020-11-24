package ru.citeck.ecos.records3.record.op.atts.service.computed

import ru.citeck.ecos.commons.data.ObjectData

data class ComputedAttDef @JvmOverloads constructor (
    val type: ComputedAttType = ComputedAttType.NONE,
    val config: ObjectData = ObjectData.create(),
    val persistent: Boolean = false
) {
    companion object {
        val EMPTY = ComputedAttDef()
    }
}
