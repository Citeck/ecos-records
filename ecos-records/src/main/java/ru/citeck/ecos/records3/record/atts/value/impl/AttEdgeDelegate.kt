package ru.citeck.ecos.records3.record.atts.value.impl

import ru.citeck.ecos.records3.record.atts.value.AttEdge

open class AttEdgeDelegate(val impl: AttEdge) : AttEdge by impl {

    private var getValue: (() -> Any?)? = null

    constructor(impl: AttEdge, getValue: () -> Any?) : this(impl) {
        this.getValue = getValue
    }

    override fun getValue(): Any? {
        val getValue = this.getValue
        return if (getValue != null) {
            getValue.invoke()
        } else {
            impl.getValue()
        }
    }
}
