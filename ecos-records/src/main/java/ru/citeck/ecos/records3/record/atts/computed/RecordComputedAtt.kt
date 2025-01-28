package ru.citeck.ecos.records3.record.atts.computed

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.ObjectData
import com.fasterxml.jackson.databind.annotation.JsonDeserialize as JackJsonDeserialize

@JsonDeserialize(builder = RecordComputedAtt.Builder::class)
@JackJsonDeserialize(builder = RecordComputedAtt.Builder::class)
data class RecordComputedAtt constructor(
    val id: String,
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
        fun create(builder: Builder.() -> Unit): RecordComputedAtt {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): RecordComputedAtt {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        var id: String = ""
        var type: RecordComputedAttType = RecordComputedAttType.NONE
        var config: ObjectData = ObjectData.create()
        var resultType: RecordComputedAttResType = RecordComputedAttResType.ANY

        constructor(base: RecordComputedAtt) : this() {
            this.id = base.id
            this.type = base.type
            this.config = base.config.deepCopy()
            this.resultType = base.resultType
        }

        fun withId(id: String): Builder {
            this.id = id
            return this
        }

        fun withType(type: RecordComputedAttType): Builder {
            this.type = type
            return this
        }

        fun withResultType(resultType: RecordComputedAttResType?): Builder {
            this.resultType = resultType ?: RecordComputedAttResType.ANY
            return this
        }

        fun withConfig(config: ObjectData): Builder {
            this.config = config
            return this
        }

        fun build(): RecordComputedAtt {
            return RecordComputedAtt(id, type, config, resultType)
        }
    }
}
