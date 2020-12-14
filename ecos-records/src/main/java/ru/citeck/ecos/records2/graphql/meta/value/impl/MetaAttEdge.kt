package ru.citeck.ecos.records2.graphql.meta.value.impl

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.graphql.meta.value.CreateVariant
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge
import ru.citeck.ecos.records2.graphql.meta.value.MetaField
import ru.citeck.ecos.records3.record.op.atts.service.value.AttEdge
import ru.citeck.ecos.records3.record.request.RequestContext

class MetaAttEdge(private val edge: AttEdge) : MetaEdge {

    override fun isProtected(): Boolean = edge.isProtected

    override fun isMultiple(): Boolean = edge.isMultiple

    override fun isAssociation(): Boolean = edge.isAssociation

    override fun isSearchable(): Boolean = edge.isSearchable

    override fun isUnreadable(): Boolean = edge.isUnreadable

    override fun getOptions(): List<*>? = edge.options

    override fun getDistinct(): List<*>? = edge.distinct

    override fun getCreateVariants(): List<CreateVariant>? = Json.mapper.convert(
        edge.createVariants,
        Json.mapper.getListType(CreateVariant::class.java)
    )

    override fun getJavaClass(): Class<*>? = edge.javaClass

    override fun getEditorKey(): String? = edge.editorKey

    override fun getType(): String? = edge.type

    override fun getTitle(): String? = MLText.getClosestValue(edge.title, RequestContext.getLocale())

    override fun getDescription(): String? = MLText.getClosestValue(edge.title, RequestContext.getLocale())

    override fun getName(): String? = edge.name

    override fun getValue(field: MetaField): Any? = edge.value
}
