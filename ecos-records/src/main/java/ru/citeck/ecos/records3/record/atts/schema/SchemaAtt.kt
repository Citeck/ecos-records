package ru.citeck.ecos.records3.record.atts.schema

import ecos.com.fasterxml.jackson210.annotation.JsonSetter
import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.records3.record.atts.proc.AttProcDef
import ru.citeck.ecos.records3.record.atts.schema.exception.AttSchemaException
import ru.citeck.ecos.records3.record.atts.schema.write.AttSchemaWriterV2
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

        const val ROOT_NAME = "ROOT"

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

    fun withName(name: String): SchemaAtt {
        if (this.name == name) {
            return this
        }
        return copy().withName(name).build()
    }

    fun withAlias(alias: String): SchemaAtt {
        if (this.alias == alias) {
            return this
        }
        return copy().withAlias(alias).build()
    }

    fun withInner(inner: List<SchemaAtt>): SchemaAtt {
        if (this.inner == inner) {
            return this
        }
        return copy().withInner(inner).build()
    }

    fun getScalarName(): String? {
        return if (isScalar()) {
            name
        } else {
            if (inner.size != 1) {
                null
            } else {
                inner[0].getScalarName()
            }
        }
    }

    fun isScalar(): Boolean {
        return name.isNotEmpty() && name[0] == '?'
    }

    fun getAliasForValue(): String {
        return alias.ifEmpty {
            name
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
        return AttSchemaWriterV2.INSTANCE.write(this)
    }

    class Builder() {

        var alias: String = ""
        var name: String = ""
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

        fun withInner(inner: Builder): Builder {
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

            if (name.isBlank()) {
                throw AttSchemaException("Attribute can't has empty name. $this")
            }

            if (alias == name) {
                alias = ""
            }

            val att = SchemaAtt(alias, name, multiple, inner, processors)

            if (att.isScalar() && inner.isNotEmpty()) {
                throw AttSchemaException("Attribute can't be a scalar and has inner attributes. $this")
            }
            if (!att.isScalar() && inner.isEmpty()) {
                throw AttSchemaException("Attribute can't be not a scalar and has empty inner attributes. $this")
            }
            if (att.isScalar() && att.multiple) {
                throw AttSchemaException("Scalar can't hold multiple values. $this")
            }
            if (att.isScalar() && ScalarType.getBySchema(att.name) == null) {
                throw AttSchemaException("Unknown scalar: '" + att.name + "'")
            }

            return att
        }

        override fun toString(): String {
            return "{" +
                "\"alias\":\"$alias\"," +
                "\"name\":\"$name\"," +
                "\"multiple\":$multiple," +
                "\"inner\":\"$inner\"," +
                "\"processors\":\"$processors\"" +
                "}"
        }
    }
}
