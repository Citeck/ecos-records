package ru.citeck.ecos.records3.iter

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonDeserialize as JackJsonDeserialize

@JsonDeserialize(builder = IterableRecordsConfig.Builder::class)
@JackJsonDeserialize(builder = IterableRecordsConfig.Builder::class)
data class IterableRecordsConfig(
    val pageSize: Int,
    val pageStrategy: PageStrategy,
    val attsToLoad: Map<String, *>
) {
    companion object {

        @JvmField
        val EMPTY = create {}

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): IterableRecordsConfig {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): IterableRecordsConfig {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        var pageSize: Int = 100
        var pageStrategy: PageStrategy = PageStrategy.SORT_BY
        var attsToLoad: Map<String, *> = emptyMap<String, Any>()

        constructor(base: IterableRecordsConfig) : this() {
            pageSize = base.pageSize
            pageStrategy = base.pageStrategy
            attsToLoad = base.attsToLoad
        }

        fun withPageSize(pageSize: Int): Builder {
            this.pageSize = pageSize
            return this
        }

        fun withPageStrategy(pageStrategy: PageStrategy): Builder {
            this.pageStrategy = pageStrategy
            return this
        }

        fun withAttsToLoad(attsToLoad: Map<String, *>): Builder {
            this.attsToLoad = attsToLoad
            return this
        }

        fun build(): IterableRecordsConfig {
            return IterableRecordsConfig(pageSize, pageStrategy, attsToLoad)
        }
    }
}
