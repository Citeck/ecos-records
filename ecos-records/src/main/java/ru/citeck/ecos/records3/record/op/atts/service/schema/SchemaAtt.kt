package ru.citeck.ecos.records3.record.op.atts.service.schema

import ecos.com.fasterxml.jackson210.annotation.JsonSetter
import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.utils.MandatoryParam
import ru.citeck.ecos.records3.record.op.atts.service.proc.AttProcDef
import ru.citeck.ecos.records3.record.op.atts.service.schema.exception.AttSchemaException
import ru.citeck.ecos.records3.record.op.atts.service.schema.write.AttSchemaGqlWriter
import com.fasterxml.jackson.annotation.JsonSetter as JackJsonSetter
import com.fasterxml.jackson.databind.annotation.JsonDeserialize as JackJsonDeserialize

@JsonDeserialize(builder = SchemaAtt.Builder::class)
@JackJsonDeserialize(builder = SchemaAtt.Builder::class)
data class SchemaAtt(
    val alias: String,
    val name: String,
    val multiple: Boolean,
    val inner: List<SchemaAtt>,
    val processors: List<AttProcDef>
) {

    companion object {

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): SchemaAtt {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun getScalarName(): String? {
        return if (isScalar()) {
            name
        } else {
            if (inner.size != 1) {
                null
            } else inner[0].getScalarName()
        }
    }

    fun isScalar(): Boolean {
        return name.isNotEmpty() && name[0] == '?'
    }

    fun getAliasForValue(): String {
        return if (alias.isEmpty()) {
            name
        } else {
            alias
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): SchemaAtt {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    override fun toString(): String {
        return AttSchemaGqlWriter.INSTANCE.write(this)
    }

    class Builder() {

        var alias: String = ""
        lateinit var name: String
        var multiple: Boolean = false
        var inner: List<SchemaAtt> = emptyList()
        var processors: List<AttProcDef> = emptyList()

        constructor(base: SchemaAtt) : this() {
            this.alias = base.alias
            this.name = base.name
            this.multiple = base.multiple
            this.inner = base.inner
            this.processors = base.processors
        }

        fun withAlias(alias: String): Builder {
            this.alias = alias
            return this
        }

        fun withName(name: String): Builder {
            this.name = name
            return this
        }

        fun withMultiple(multiple: Boolean): Builder {
            this.multiple = multiple
            return this
        }

        fun withInner(inner: SchemaAtt.Builder): Builder {
            return withInner(inner.build())
        }

        fun withInner(inner: SchemaAtt): Builder {
            return withInner(listOf(inner))
        }

        @JsonSetter
        @JackJsonSetter
        fun withInner(inner: List<SchemaAtt>): Builder {
            this.inner = inner
            return this
        }

        fun withProcessors(processors: List<AttProcDef>): Builder {
            this.processors = processors
            return this
        }

        fun build(): SchemaAtt {

            MandatoryParam.check("name", name)
            val att = SchemaAtt(alias, name, multiple, inner, processors)

            if (att.isScalar() && !inner.isEmpty()) {
                throw AttSchemaException("Attribute can't be a scalar and has inner attributes. $this")
            }
            if (!att.isScalar() && inner.isEmpty()) {
                throw AttSchemaException("Attribute can't be not a scalar and has empty inner attributes. $this")
            }
            if (att.isScalar() && att.multiple) {
                throw AttSchemaException("Scalar can't hold multiple values. $this")
            }
            if (att.isScalar() && !ScalarType.getBySchema(att.name).isPresent) {
                throw AttSchemaException("Unknown scalar: '" + att.name + "'")
            }

            return att
        }
    }
}
