package ru.citeck.ecos.records3.record.op.atts.service.value.impl.meta

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge
import ru.citeck.ecos.records2.graphql.meta.value.field.AttMetaField
import ru.citeck.ecos.records3.record.op.atts.dto.CreateVariant
import ru.citeck.ecos.records3.record.op.atts.service.value.AttEdge

/**
 * Adapter MetaEdge -> AttEdge
 */
class AttMetaEdge(val edge: MetaEdge) : AttEdge {

    override fun isProtected(): Boolean = edge.isProtected

    override fun isMultiple(): Boolean = edge.isMultiple

    override fun isAssociation(): Boolean = edge.isAssociation

    override fun isSearchable(): Boolean = edge.isSearchable

    override fun isReadable(): Boolean = edge.isReadable

    override fun getOptions(): List<*>? = edge.options

    override fun getDistinct(): List<*>? = edge.distinct

    override fun getCreateVariants(): List<CreateVariant>? {
        val variants = edge.createVariants ?: return null
        return DataValue.create(variants).asList(CreateVariant::class.java)
    }

    override fun getJavaClass(): Class<*>? = edge.javaClass

    override fun getEditorKey(): String? = edge.editorKey

    override fun getType(): String? = edge.type

    override fun getTitle(): MLText? {
        val value = edge.title
        return if (value != null) {
            MLText(value)
        } else {
            null
        }
    }

    override fun getDescription(): MLText? {
        val value = edge.description
        return if (value != null) {
            MLText(value)
        } else {
            null
        }
    }

    override fun getName(): String? = edge.name

    override fun getValue(): Any? = edge.getValue(AttMetaField)
}
