package ru.citeck.ecos.records2.graphql.meta.value.impl

import ecos.com.fasterxml.jackson210.databind.node.NullNode
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.utils.LibsUtils
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge
import ru.citeck.ecos.records2.graphql.meta.value.MetaField
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.request.RequestContext
import com.fasterxml.jackson.databind.node.NullNode as JackNullNode

class MetaAttValue(private val attValue: AttValue) : MetaValue {

    override fun getString(): String? = attValue.asText()

    override fun getDisplayName(): String? {
        val disp = attValue.displayName ?: return null

        if (LibsUtils.isJacksonPresent()) {
            if (disp is JackNullNode) {
                return null
            }
        }
        if (disp is DataValue && disp.isNull()) {
            return null
        }
        return when (disp) {
            is NullNode -> null
            is String -> disp
            is MLText -> MLText.getClosestValue(disp, RequestContext.getLocale())
            else -> disp.toString()
        }
    }

    override fun getId(): String? = attValue.id?.toString() ?: ""

    override fun getLocalId(): String = RecordRef.valueOf(attValue.id?.toString() ?: "").id

    override fun getAttribute(name: String, field: MetaField): Any? = attValue.getAtt(name)

    override fun has(name: String): Boolean = attValue.has(name)

    override fun getDouble(): Double? = attValue.asDouble()

    override fun getBool(): Boolean? = attValue.asBoolean()

    override fun getJson(): Any? = attValue.asJson()

    override fun getAs(type: String, field: MetaField) = attValue.getAs(type)

    override fun getAs(type: String) = attValue.getAs(type)

    override fun getRecordType(): RecordRef = attValue.type

    override fun getEdge(name: String, field: MetaField): MetaEdge? {
        val edge = attValue.getEdge(name) ?: return null
        return MetaAttEdge(edge)
    }
}
