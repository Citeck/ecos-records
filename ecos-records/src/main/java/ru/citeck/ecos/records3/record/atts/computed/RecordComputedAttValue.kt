package ru.citeck.ecos.records3.record.atts.computed

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.ObjectData
import com.fasterxml.jackson.databind.annotation.JsonDeserialize as JackJsonDeserialize

/**
 * This value will be evaluated as computed attribute in AttSchemaResolver
 */
@JsonDeserialize(builder = RecordComputedAttValue.Builder::class)
@JackJsonDeserialize(builder = RecordComputedAttValue.Builder::class)
data class RecordComputedAttValue(
    val type: RecordComputedAttType,
    val config: ObjectData,
    val resultType: RecordComputedAttResType
) {
    companion object {

        @JvmField
        val EMPTY = create {}

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): RecordComputedAttValue {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): RecordComputedAttValue {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        var type: RecordComputedAttType = RecordComputedAttType.NONE
        var config: ObjectData = ObjectData.create()
        var resultType: RecordComputedAttResType = RecordComputedAttResType.ANY

        constructor(base: RecordComputedAttValue) : this() {
            this.type = base.type
            this.config = base.config.deepCopy()
            this.resultType = base.resultType
        }

        fun withType(type: RecordComputedAttType): Builder {
            this.type = type
            return this
        }

        fun withConfig(config: ObjectData): Builder {
            this.config = config
            return this
        }

        fun withResultType(resultType: RecordComputedAttResType): Builder {
            this.resultType = resultType
            return this
        }

        fun build(): RecordComputedAttValue {
            return RecordComputedAttValue(type, config, resultType)
        }
    }
}
