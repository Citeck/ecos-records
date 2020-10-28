package ru.citeck.ecos.records3.record.op.atts.service.value.impl.meta

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue
import ru.citeck.ecos.records2.graphql.meta.value.field.EmptyMetaField
import ru.citeck.ecos.records3.record.op.atts.service.value.AttEdge
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue

/**
 * Adapter MetaValue -> AttValue
 */
class AttMetaValue(private val metaValue: MetaValue) : AttValue {

    override fun getId(): Any? {
        return metaValue.id
    }

    override fun asText(): String? {
        return metaValue.string
    }

    override fun getDisplayName(): String? {
        return metaValue.displayName
    }

    @Throws(Exception::class)
    override fun getAtt(name: String): Any? {
        return metaValue.getAttribute(name, EmptyMetaField.INSTANCE)
    }

    override fun getEdge(name: String): AttEdge? {
        return AttMetaEdge(metaValue.getEdge(name, EmptyMetaField.INSTANCE))
    }

    @Throws(Exception::class)
    override fun has(name: String): Boolean {
        return metaValue.has(name)
    }

    override fun asDouble(): Double? {
        return metaValue.double
    }

    override fun asBoolean(): Boolean? {
        return metaValue.bool
    }

    override fun asJson(): Any? {
        return metaValue.json
    }

    override fun getAs(type: String): Any? {
        return metaValue.getAs(type, EmptyMetaField.INSTANCE)
    }

    override fun getType(): RecordRef {
        return metaValue.recordType
    }
}
