package ru.citeck.ecos.records3.record.dao.query.dto.query

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import mu.KotlinLogging
import ru.citeck.ecos.records2.RecordRef
import com.fasterxml.jackson.databind.annotation.JsonDeserialize as JackJsonDeserialize

@JsonDeserialize(builder = QueryPage.Builder::class)
@JackJsonDeserialize(builder = QueryPage.Builder::class)
data class QueryPage(
    val maxItems: Int,
    val skipCount: Int,
    val afterId: RecordRef
) {

    companion object {

        val log = KotlinLogging.logger {}

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): QueryPage {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): QueryPage {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        var maxItems: Int = -1
        var skipCount: Int = 0
        var afterId: RecordRef = RecordRef.EMPTY

        constructor(base: QueryPage) : this() {
            maxItems = base.maxItems
            skipCount = base.skipCount
            afterId = base.afterId
        }

        fun withMaxItems(maxItems: Int?): Builder {
            this.maxItems = maxItems ?: -1
            return this
        }

        fun withSkipCount(skipCount: Int?): Builder {
            this.skipCount = skipCount ?: 0
            return this
        }

        fun withAfterId(afterId: RecordRef?): Builder {
            this.afterId = afterId ?: RecordRef.EMPTY
            return this
        }

        fun build(): QueryPage {
            if (skipCount < 0) {
                log.warn { "Skip count can't be less than zero. Actual value: $skipCount will be replaced with 0" }
                skipCount = 0
            }
            if (skipCount > 0 && afterId != RecordRef.EMPTY) {
                log.warn(
                    "Skip count can't be used when afterId is set. " +
                        "skipCount: $skipCount afterId: $afterId. Skip count will be replaced with 0"
                )
                skipCount = 0
            }
            return QueryPage(maxItems, skipCount, afterId)
        }
    }
}
