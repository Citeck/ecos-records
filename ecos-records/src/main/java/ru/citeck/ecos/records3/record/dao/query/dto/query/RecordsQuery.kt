package ru.citeck.ecos.records3.record.dao.query.dto.query

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore
import ecos.com.fasterxml.jackson210.annotation.JsonSetter
import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import java.util.*
import com.fasterxml.jackson.annotation.JsonIgnore as JackJsonIgnore
import com.fasterxml.jackson.annotation.JsonSetter as JackJsonSetter
import com.fasterxml.jackson.databind.annotation.JsonDeserialize as JackJsonDeserialize

@IncludeNonDefault
@JsonDeserialize(builder = RecordsQuery.Builder::class)
@JackJsonDeserialize(builder = RecordsQuery.Builder::class)
data class RecordsQuery(
    val sourceId: String,
    val ecosType: String,
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
        val result = getQueryOrNull(type)
        if (result == null) {
            log.warn("Can't convert query to type $type. Query: $query")
            return type.newInstance()
        }
        return result
    }

    @JsonIgnore
    @JackJsonIgnore
    fun isAfterIdMode(): Boolean {
        return page.afterId != null
    }

    fun withSourceId(sourceId: String): RecordsQuery {
        if (this.sourceId == sourceId) {
            return this
        }
        return copy().withSourceId(sourceId).build()
    }

    fun withQuery(query: DataValue): RecordsQuery {
        return copy().withQuery(query).build()
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
        var ecosType: String = ""
        var sortBy: MutableList<SortBy> = arrayListOf()
        var groupBy: MutableList<String> = arrayListOf()
        var page: QueryPage.Builder = QueryPage.create()
        var consistency: Consistency = Consistency.DEFAULT
        var language: String = ""
        var query: DataValue = DataValue.NULL

        constructor(base: RecordsQuery) : this() {
            sourceId = base.sourceId
            ecosType = base.ecosType
            sortBy = DataValue.create(base.sortBy).asList(SortBy::class.java)
            groupBy = ArrayList(base.groupBy)
            page = base.page.copy()
            consistency = base.consistency
            language = base.language
            query = base.query
        }

        fun withSourceId(sourceId: String?): Builder {
            this.sourceId = sourceId ?: ""
            return this
        }

        fun withEcosType(ecosType: String?): Builder {
            this.ecosType = ecosType ?: ""
            return this
        }

        fun withSortBy(sortBy: SortBy?): Builder {
            this.sortBy = sortBy?.let { arrayListOf(it) } ?: arrayListOf()
            return this
        }

        @JsonSetter
        @JackJsonSetter
        fun withSortBy(sortBy: List<SortBy>?): Builder {
            this.sortBy = sortBy?.let { ArrayList(it) } ?: arrayListOf()
            return this
        }

        fun withGroupBy(groupBy: List<String>?): Builder {
            this.groupBy = groupBy?.let { ArrayList(it) } ?: arrayListOf()
            return this
        }

        fun withPage(page: QueryPage?): Builder {
            this.page = page?.copy() ?: QueryPage.create()
            return this
        }

        fun withMaxItems(maxItems: Int?): Builder {
            this.page.withMaxItems(maxItems)
            return this
        }

        fun withSkipCount(skipCount: Int?): Builder {
            this.page.withSkipCount(skipCount)
            return this
        }

        fun withAfterId(afterId: RecordRef?): Builder {
            this.page.withAfterId(afterId)
            return this
        }

        fun withConsistency(consistency: Consistency?): Builder {
            this.consistency = consistency ?: Consistency.DEFAULT
            return this
        }

        fun withLanguage(language: String?): Builder {
            this.language = language ?: ""
            return this
        }

        fun withQuery(query: Any?): Builder {
            if (language == "" && query is Predicate) {
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
            }
            this.query = query as? DataValue ?: DataValue.create(query)
            return this
        }

        fun addSort(sort: SortBy): Builder {
            sortBy.add(sort)
            return this
        }

        fun build(): RecordsQuery {
            return RecordsQuery(
                sourceId,
                ecosType,
                sortBy,
                groupBy,
                page.build(),
                consistency,
                language,
                query
            )
        }
    }
}
