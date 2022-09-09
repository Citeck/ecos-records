package ru.citeck.ecos.records3

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize

@JsonDeserialize(builder = RecordsProperties.Builder::class)
class RecordsProperties(
    val sourceIdMapping: Map<String, String>,
    val defaultApp: String,
    val legacyApiMode: Boolean
) {
    companion object {

        @JvmField
        val DEFAULT = create {}

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): RecordsProperties {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): RecordsProperties {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    fun withDefaultApp(defaultApp: String?): RecordsProperties {
        return copy { withDefaultApp(defaultApp) }
    }

    class Builder() {

        var sourceIdMapping: Map<String, String> = emptyMap()
        var defaultApp: String = ""
        var legacyApiMode: Boolean = false

        constructor(base: RecordsProperties) : this() {
            sourceIdMapping = base.sourceIdMapping
            defaultApp = base.defaultApp
            legacyApiMode = base.legacyApiMode
        }

        fun withSourceIdMapping(sourceIdMapping: Map<String, String>?): Builder {
            this.sourceIdMapping = sourceIdMapping ?: DEFAULT.sourceIdMapping
            return this
        }

        fun withDefaultApp(defaultApp: String?): Builder {
            this.defaultApp = defaultApp ?: DEFAULT.defaultApp
            return this
        }

        fun withLegacyApiMode(legacyApiMode: Boolean?): Builder {
            this.legacyApiMode = legacyApiMode ?: false
            return this
        }

        fun build(): RecordsProperties {
            return RecordsProperties(sourceIdMapping, defaultApp, legacyApiMode)
        }
    }
}
