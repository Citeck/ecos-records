package ru.citeck.ecos.records2.graphql.meta.value.impl

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge
import ru.citeck.ecos.records2.graphql.meta.value.MetaField
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue

class MetaAttValue(private val attValue: AttValue) : MetaValue {

    override fun getString(): String? = attValue.asText()

    override fun getDisplayName(): String? = attValue.displayName

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
