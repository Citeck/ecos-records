package ru.citeck.ecos.records3.record.op.atts.service.schema.resolver

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.op.atts.service.mixin.AttMixin
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt

data class ResolveArgs(
    val values: List<Any?>,
    val sourceId: String,
    val valueRefs: List<RecordRef>,
    val attributes: List<SchemaAtt>,
    val mixins: List<AttMixin>,
    val rawAtts: Boolean
) {

    companion object {

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): ResolveArgs {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): ResolveArgs {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        var values: List<Any?> = emptyList()
        var sourceId: String = ""
        var valueRefs: List<RecordRef> = emptyList()
        var attributes: List<SchemaAtt> = emptyList()
        var mixins: List<AttMixin> = emptyList()
        var rawAtts = false

        constructor(base: ResolveArgs) : this() {
            values = ArrayList(base.values)
            sourceId = base.sourceId
            valueRefs = ArrayList(base.valueRefs)
            attributes = ArrayList(base.attributes)
            mixins = ArrayList(base.mixins)
            rawAtts = base.rawAtts
        }

        fun withValues(values: List<*>): Builder {
            this.values = values
            return this
        }

        fun withSourceId(sourceId: String): Builder {
            this.sourceId = sourceId
            return this
        }

        fun withValueRefs(valueRefs: List<RecordRef>): Builder {
            this.valueRefs = valueRefs
            return this
        }

        fun withAttribute(attribute: SchemaAtt): Builder {
            return withAttributes(listOf(attribute))
        }

        fun withAttributes(attributes: List<SchemaAtt>): Builder {
            this.attributes = attributes
            return this
        }

        fun withMixins(mixins: List<AttMixin>): Builder {
            this.mixins = mixins
            return this
        }

        fun withRawAtts(rawAtts: Boolean): Builder {
            this.rawAtts = rawAtts
            return this
        }

        fun build(): ResolveArgs {
            if (valueRefs.isNotEmpty() && valueRefs.size != values.size) {
                throw RuntimeException("valueRefs should have same size with values")
            }
            return ResolveArgs(values, sourceId, valueRefs, attributes, mixins, rawAtts)
        }
    }
}
