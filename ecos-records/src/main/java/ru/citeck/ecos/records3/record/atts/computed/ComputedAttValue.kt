package ru.citeck.ecos.records3.record.atts.computed

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.ObjectData
import com.fasterxml.jackson.databind.annotation.JsonDeserialize as JackJsonDeserialize

/**
 * This value will be evaluated as computed attribute in AttSchemaResolver
 */
@JsonDeserialize(builder = ComputedAttValue.Builder::class)
@JackJsonDeserialize(builder = ComputedAttValue.Builder::class)
class ComputedAttValue(
    val type: ComputedAttType,
    val config: ObjectData
) {
    companion object {

        @JvmField
        val EMPTY = create {}

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): ComputedAttValue {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): ComputedAttValue {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        var type: ComputedAttType = ComputedAttType.NONE
        var config: ObjectData = ObjectData.create()

        constructor(base: ComputedAttValue) : this() {
            this.type = base.type
            this.config = base.config.deepCopy()
        }

        fun withType(type: ComputedAttType): Builder {
            this.type = type
            return this
        }

        fun withConfig(config: ObjectData): Builder {
            this.config = config
            return this
        }

        fun build(): ComputedAttValue {
            return ComputedAttValue(type, config)
        }
    }
}
