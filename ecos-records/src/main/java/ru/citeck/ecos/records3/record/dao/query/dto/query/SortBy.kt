package ru.citeck.ecos.records3.record.dao.query.dto.query

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonDeserialize as JackJsonDeserialize

@JsonDeserialize(builder = SortBy.Builder::class)
@JackJsonDeserialize(builder = SortBy.Builder::class)
data class SortBy(
    val attribute: String,
    val ascending: Boolean
) {

    companion object {

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): SortBy {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): SortBy {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        var attribute: String = ""
        var ascending: Boolean = false

        constructor(base: SortBy) : this() {
            attribute = base.attribute
            ascending = base.ascending
        }

        fun build(): SortBy {
            return SortBy(attribute, ascending)
        }
    }
}
