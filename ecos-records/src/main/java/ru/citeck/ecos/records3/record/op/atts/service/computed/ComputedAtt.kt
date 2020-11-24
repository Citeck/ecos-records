package ru.citeck.ecos.records3.record.op.atts.service.computed

data class ComputedAtt @JvmOverloads constructor (
    val id: String = "",
    val def: ComputedAttDef = ComputedAttDef()
) {
    companion object {
        @JvmField
        val EMPTY = ComputedAtt()
    }
}
