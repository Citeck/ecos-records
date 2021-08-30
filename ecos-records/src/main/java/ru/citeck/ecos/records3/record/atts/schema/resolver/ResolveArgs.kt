package ru.citeck.ecos.records3.record.atts.schema.resolver

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.mixin.EmptyMixinContext
import ru.citeck.ecos.records3.record.mixin.MixinContext

data class ResolveArgs(
    val values: List<Any?>,
    val sourceId: String,
    val valueRefs: List<RecordRef>,
    val attributes: List<SchemaAtt>,
    val mixinCtx: MixinContext,
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
        var mixinCtx: MixinContext = EmptyMixinContext
        var rawAtts = false

        constructor(base: ResolveArgs) : this() {
            values = ArrayList(base.values)
            sourceId = base.sourceId
            valueRefs = ArrayList(base.valueRefs)
            attributes = ArrayList(base.attributes)
            mixinCtx = base.mixinCtx
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

        fun withMixinContext(mixinCtx: MixinContext): Builder {
            this.mixinCtx = mixinCtx
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
            return ResolveArgs(values, sourceId, valueRefs, attributes, mixinCtx, rawAtts)
        }
    }
}
