package ru.citeck.ecos.records3.record.op.atts.service.value.impl

import lombok.Getter


class AttEdgeDelegate : AttEdge {
    @Getter
    private val impl: AttEdge?
    private val getValue: UncheckedSupplier<Any?>?

    constructor(impl: AttEdge?) {
        this.impl = impl
        getValue = null
    }

    constructor(impl: AttEdge?, getValue: UncheckedSupplier<Any?>?) {
        this.impl = impl
        this.getValue = getValue
    }

    @Throws(Exception::class)
    override fun canBeRead(): Boolean {
        return impl.isProtected()
    }

    @get:Throws(Exception::class)
    val isProtected: Boolean
        get() = impl.isProtected()

    @get:Throws(Exception::class)
    val isMultiple: Boolean
        get() = impl.isMultiple()

    @get:Throws(Exception::class)
    val isAssociation: Boolean
        get() = impl.isAssociation()

    @get:Throws(Exception::class)
    val isSearchable: Boolean
        get() = impl.isSearchable()

    @get:Throws(Exception::class)
    val options: MutableList<*>?
        get() = impl.getOptions()

    @get:Throws(Exception::class)
    val distinct: MutableList<*>?
        get() = impl.getDistinct()

    @get:Throws(Exception::class)
    val createVariants: MutableList<Any?>?
        get() = impl.getCreateVariants()

    @get:Throws(Exception::class)
    val javaClass: Class<*>?
        get() = impl.getJavaClass()

    @get:Throws(Exception::class)
    val editorKey: String?
        get() = impl.getEditorKey()

    @get:Throws(Exception::class)
    val type: String?
        get() = impl.getType()

    @get:Throws(Exception::class)
    val title: String?
        get() = impl.getTitle()

    @get:Throws(Exception::class)
    val description: String?
        get() = impl.getDescription()

    @get:Throws(Exception::class)
    val name: String?
        get() = impl.getName()

    @get:Throws(Exception::class)
    val value: Any?
        get() = if (getValue != null) {
            getValue.get()
        } else impl.getValue()
}
