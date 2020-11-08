package ru.citeck.ecos.records3.record.op.query.dto.query

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore
import ecos.com.fasterxml.jackson210.annotation.JsonSetter
import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.request.query.SortBy
import java.util.*
import com.fasterxml.jackson.annotation.JsonIgnore as JackJsonIgnore
import com.fasterxml.jackson.annotation.JsonSetter as JackJsonSetter
import com.fasterxml.jackson.databind.annotation.JsonDeserialize as JackJsonDeserialize

@JsonDeserialize(builder = RecordsQuery.Builder::class)
@JackJsonDeserialize(builder = RecordsQuery.Builder::class)
data class RecordsQuery(
    val sourceId: String,
    val sortBy: List<SortBy>,
    val groupBy: List<String>,
    val page: QueryPage,
    val consistency: Consistency,
    val language: String,
    val query: DataValue
) {

    companion object {

        val log = KotlinLogging.logger {}

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): RecordsQuery {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun <T : Any> getQueryOrNull(type: Class<T>): T? {
        return Json.mapper.convert(query, type)
    }

    fun <T : Any> getQuery(type: Class<T>): T {
        var result = Json.mapper.convert(query, type)
        if (result == null) {
            log.warn("Can't convert query to type $type. Query: $query")
            result = type.newInstance()
        }
        return result!!
    }

    @JsonIgnore
    @JackJsonIgnore
    fun isAfterIdMode(): Boolean {
        return page.afterId != RecordRef.EMPTY
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): RecordsQuery {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        var sourceId: String = ""
        var sortBy: MutableList<SortBy> = arrayListOf()
        var groupBy: MutableList<String> = arrayListOf()
        var page: QueryPage.Builder = QueryPage.create()
        var consistency: Consistency = Consistency.DEFAULT
        var language: String = ""
        var query: DataValue = DataValue.NULL

        constructor(base: RecordsQuery) : this() {
            sourceId = base.sourceId
            sortBy = DataValue.create(base.sortBy).asList(SortBy::class.java)
            groupBy = ArrayList(base.groupBy)
            page = base.page.copy()
            consistency = base.consistency
            language = base.language
            query = base.query
        }

        fun withSourceId(sourceId: String): Builder {
            this.sourceId = sourceId
            return this
        }

        fun withSortBy(sortBy: SortBy): Builder {
            this.sortBy = arrayListOf(sortBy)
            return this
        }

        @JsonSetter
        @JackJsonSetter
        fun withSortBy(sortBy: List<SortBy>): Builder {
            this.sortBy = ArrayList(sortBy)
            return this
        }

        fun withGroupBy(groupBy: List<String>): Builder {
            this.groupBy = ArrayList(groupBy)
            return this
        }

        fun withPage(page: QueryPage): Builder {
            this.page = page.copy()
            return this
        }

        fun withMaxItems(maxItems: Int): Builder {
            this.page.withMaxItems(maxItems)
            return this
        }

        fun withSkipCount(skipCount: Int): Builder {
            this.page.withSkipCount(skipCount)
            return this
        }

        fun withAfterId(afterId: RecordRef): Builder {
            this.page.withAfterId(afterId)
            return this
        }

        fun withConsistency(consistency: Consistency): Builder {
            this.consistency = consistency
            return this
        }

        fun withLanguage(language: String): Builder {
            this.language = language
            return this
        }

        fun withQuery(query: Any?): Builder {
            this.query = query as? DataValue ?: DataValue.create(query)
            return this
        }

        fun addSort(sort: SortBy): Builder {
            sortBy.add(sort)
            return this
        }

        fun build(): RecordsQuery {
            return RecordsQuery(sourceId, sortBy, groupBy, page.build(), consistency, language, query)
        }
    }
}
