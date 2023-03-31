package ru.citeck.ecos.records3.record.atts.value.impl

import ru.citeck.ecos.records3.record.atts.value.AttEdge

open class AttEdgeDelegate(val impl: AttEdge) : AttEdge {

    private var getValue: (() -> Any?)? = null

    constructor(impl: AttEdge, getValue: () -> Any?) : this(impl) {
        this.getValue = getValue
    }

    override fun getValue(): Any? {
        val getValue = this.getValue
        return if (getValue != null) {
            getValue.invoke()
        } else {
            impl.value
        }
    }

    override fun isProtected() = impl.isProtected
    override fun isMandatory() = impl.isMandatory
    override fun isMultiple() = impl.isMultiple
    override fun isAssociation() = impl.isAssociation
    override fun isSearchable() = impl.isSearchable
    override fun isUnreadable() = impl.isUnreadable
    override fun getOptions(): List<*>? = impl.options
    override fun getDistinct(): List<*>? = impl.distinct
    override fun getCreateVariants(): List<*>? = impl.createVariants
    override fun getJavaClass() = impl.javaClass
    override fun getEditorKey() = impl.editorKey
    override fun getType() = impl.type
    override fun getTitle() = impl.title
    override fun getDescription() = impl.description
    override fun getName() = impl.name
}
