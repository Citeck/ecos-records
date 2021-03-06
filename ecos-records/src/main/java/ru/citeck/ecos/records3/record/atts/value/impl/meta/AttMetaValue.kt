package ru.citeck.ecos.records3.record.atts.value.impl.meta

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue
import ru.citeck.ecos.records2.graphql.meta.value.field.AttMetaField
import ru.citeck.ecos.records3.record.atts.value.AttEdge
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.utils.AttUtils

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
        return metaValue.getAttribute(name, AttMetaField)
    }

    override fun getEdge(name: String): AttEdge? {
        val metaEdge = metaValue.getEdge(name, AttMetaField)
        return metaEdge?.let { AttMetaEdge(it) }
    }

    @Throws(Exception::class)
    override fun has(name: String): Boolean {
        return metaValue.has(name)
    }

    override fun asDouble(): Double? {
        return try {
            metaValue.double
        } catch (e: NumberFormatException) {
            AttUtils.logError(
                this,
                "Number format exception: " + e.message
            )
            null
        }
    }

    override fun asBoolean(): Boolean? {
        return metaValue.bool
    }

    override fun asJson(): Any? {
        return metaValue.json
    }

    override fun getAs(type: String): Any? {
        return metaValue.getAs(type, AttMetaField)
    }

    override fun getType(): RecordRef {
        return metaValue.recordType
    }
}
