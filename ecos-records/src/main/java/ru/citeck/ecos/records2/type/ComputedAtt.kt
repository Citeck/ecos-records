package ru.citeck.ecos.records2.type

import com.fasterxml.jackson.annotation.JsonSetter as JackJsonSetter
import ecos.com.fasterxml.jackson210.annotation.JsonSetter
import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonDeserialize as JackJsonDeserialize
import ru.citeck.ecos.commons.data.ObjectData

@JsonDeserialize(builder = ComputedAtt.Builder::class)
@JackJsonDeserialize(builder = ComputedAtt.Builder::class)
data class ComputedAtt(
    val id: String,
    val type: String,
    val config: ObjectData = ObjectData.create(),
    val persistent: Boolean = false
) {

    companion object {

        @JvmStatic
        fun create() : Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit) : ComputedAtt {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy() : Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit) : ComputedAtt {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        lateinit var id: String
        lateinit var type: String
        var config: ObjectData = ObjectData.create()
        var persistent: Boolean = false

        constructor(base: ComputedAtt) : this() {
            this.id = base.id
            this.type = base.type
            this.config = ObjectData.deepCopyOrNew(base.config)
            this.persistent = base.persistent
        }

        fun withId(id: String) : Builder {
            this.id = id
            return this
        }

        fun withType(type: String) : Builder {
            this.type = type
            return this
        }

        fun withConfig(config: ObjectData) : Builder {
            this.config = config
            return this
        }

        fun withPersistent(persistent: Boolean) : Builder {
            this.persistent = persistent
            return this
        }

        fun build() : ComputedAtt {
            return ComputedAtt(id, type, config, persistent)
        }
    }
}
