package ru.citeck.ecos.records3.record.op.atts.service.value.impl

import mu.KotlinLogging
import ru.citeck.ecos.commons.utils.func.UncheckedSupplier
import ru.citeck.ecos.records3.record.op.atts.service.value.AttEdge
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue

open class SimpleAttEdge : AttEdge {

    companion object {
        val log = KotlinLogging.logger {}
    }

    private val name: String
    private val scope: AttValue?
    private val getValueFunc: UncheckedSupplier<Any?>?

    constructor(name: String, getValueFunc: UncheckedSupplier<Any?>) {
        this.name = name
        this.getValueFunc = getValueFunc
        scope = null
    }

    constructor(name: String, scope: AttValue) {
        this.name = name
        this.scope = scope
        getValueFunc = null
    }

    override fun getName(): String {
        return name
    }

    override fun getValue(): Any? {
        return when {
            getValueFunc != null -> getValueFunc.get()
            scope != null -> scope.getAtt(name)
            else -> {
                log.warn("Scope and getValueFunc is null")
                null
            }
        }
    }
}
