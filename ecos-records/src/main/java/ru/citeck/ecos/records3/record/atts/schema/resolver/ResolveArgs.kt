package ru.citeck.ecos.records3.record.atts.schema.resolver

import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.mixin.EmptyMixinContext
import ru.citeck.ecos.records3.record.mixin.MixinContext
import ru.citeck.ecos.webapp.api.entity.EntityRef

data class ResolveArgs(
    val values: List<Any?>,
    val sourceId: String,
    val defaultValueRefs: List<EntityRef>,
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
        var defaultValueRefs: List<EntityRef> = emptyList()
        var attributes: List<SchemaAtt> = emptyList()
        var mixinCtx: MixinContext = EmptyMixinContext
        var rawAtts = false

        constructor(base: ResolveArgs) : this() {
            values = ArrayList(base.values)
            sourceId = base.sourceId
            defaultValueRefs = ArrayList(base.defaultValueRefs)
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

        fun withDefaultValueRefs(defaultValueRefs: List<EntityRef>): Builder {
            this.defaultValueRefs = defaultValueRefs
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
            if (defaultValueRefs.isNotEmpty() && defaultValueRefs.size != values.size) {
                throw RuntimeException("defaultValueRefs should have same size with values")
            }
            return ResolveArgs(
                values = values,
                sourceId = sourceId,
                defaultValueRefs = defaultValueRefs,
                attributes = attributes,
                mixinCtx = mixinCtx,
                rawAtts = rawAtts
            )
        }
    }
}
