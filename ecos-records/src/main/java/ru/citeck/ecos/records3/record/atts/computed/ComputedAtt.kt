package ru.citeck.ecos.records3.record.atts.computed

data class ComputedAtt @JvmOverloads constructor (
    val id: String = "",
    val def: ComputedAttDef = ComputedAttDef.EMPTY
) {
    companion object {
        @JvmField
        val EMPTY = ComputedAtt()
    }
}
