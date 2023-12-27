package ru.citeck.ecos.records3.iter

import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef
import java.util.*
import kotlin.collections.HashMap

class IterableRecords(
    query: RecordsQuery,
    private val config: IterableRecordsConfig = IterableRecordsConfig.EMPTY,
    private val recordsService: RecordsService
) : Iterable<RecordAtts> {

    companion object {
        private const val SORT_BY_ATT_ALIAS = "__sort_by_att__"

        const val STATE_BASE_CURRENT_IDX = "currentIdx"
        const val STATE_BASE_RECORDS = "records"
        const val STATE_BASE_PROCESSED_COUNT = "processedCount"
        const val STATE_BASE_TOTAL_COUNT = "totalCount"

        const val STATE_IT_BY_ATT_SKIP_COUNT = "skipCount"
        const val STATE_IT_BY_ATT_LAST_VALUE = "lastValue"

        const val STATE_BASE_LAST_ID = "lastId"

        private val log = KotlinLogging.logger {}
    }

    private val baseQuery: RecordsQuery = query.copy().build()

    init {
        if (config.pageStrategy == PageStrategy.CREATED) {
            if (query.language != PredicateService.LANGUAGE_PREDICATE) {
                error(
                    "${PageStrategy.CREATED} strategy can be used " +
                        "only with '${PredicateService.LANGUAGE_PREDICATE}' language"
                )
            }
        }
    }

    override fun iterator(): RecordsIterator<RecordAtts> {
        return when (config.pageStrategy) {
            PageStrategy.CREATED -> ItByAttRecordsIterator(RecordConstants.ATT_CREATED)
            PageStrategy.MODIFIED -> ItByAttRecordsIterator(RecordConstants.ATT_MODIFIED)
            PageStrategy.AFTER_ID -> AfterIdRecordsIterator()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as IterableRecords
        return baseQuery == that.baseQuery && config == that.config
    }

    override fun hashCode(): Int {
        return Objects.hash(baseQuery, config)
    }

    private fun queryRecords(query: RecordsQuery, attributes: Map<String, *>): Page {
        return if (attributes.isEmpty()) {
            val queryRes = recordsService.query(query)
            Page(queryRes.getRecords().map { RecordAtts(it) }, queryRes.getTotalCount())
        } else {
            val queryRes = recordsService.query(query, attributes)
            Page(queryRes.getRecords(), queryRes.getTotalCount())
        }
    }

    private inner class ItByAttRecordsIterator(attribute: String) : AbstractIterator() {

        private val pageSort: SortBy
        private var skipCount = 0
        private var lastValue: DataValue = DataValue.NULL

        private val basePredicate = baseQuery.getQuery(Predicate::class.java)

        private val query: RecordsQuery
        private val attsToLoad: Map<String, *>

        init {

            pageSort = baseQuery.sortBy.firstOrNull {
                it.attribute == attribute
            } ?: SortBy(attribute, false)

            query = baseQuery.copy()
                .withSortBy(pageSort)
                .withMaxItems(config.pageSize)
                .build()

            val attsToLoad = HashMap(config.attsToLoad)
            attsToLoad[SORT_BY_ATT_ALIAS] = pageSort.attribute + ScalarType.STR.schema
            this.attsToLoad = attsToLoad
        }

        override fun getState(full: Boolean): ObjectData {
            return super.getState(full)
                .set(STATE_IT_BY_ATT_LAST_VALUE, lastValue)
                .set(STATE_IT_BY_ATT_SKIP_COUNT, skipCount)
        }

        override fun setState(state: ObjectData) {
            super.setState(state)
            skipCount = state.get(STATE_IT_BY_ATT_SKIP_COUNT, 0)
            lastValue = state[STATE_IT_BY_ATT_LAST_VALUE]
        }

        override fun takeNextRecords(): Page {

            var pagePredicate = basePredicate
            if (!lastValue.isNull()) {
                pagePredicate = Predicates.and(
                    pagePredicate,
                    if (pageSort.ascending) {
                        ValuePredicate(pageSort.attribute, ValuePredicate.Type.GE, lastValue)
                    } else {
                        ValuePredicate(pageSort.attribute, ValuePredicate.Type.LE, lastValue)
                    }
                )
            }

            val query = query.copy()
                .withQuery(pagePredicate)
                .withSkipCount(skipCount)
                .build()

            val page = queryRecords(query, attsToLoad)
            val records = page.records

            if (records.isNotEmpty()) {
                if (records.any { it.getId() == lastId }) {
                    log.error {
                        "records query returned the same record: '$lastId'. " +
                            "lastValue: $lastValue " +
                            "query: $query " +
                            "attsToLoad: $attsToLoad " +
                            "records: ${records.map { it.getId() }} " +
                            "pageSort: $pageSort " +
                            "Processing will be stopped. " +
                            "Records DAO should support " +
                            "querying and sorting by attribute " + pageSort.attribute + " " +
                            "and queries with same arguments should not return different results"
                    }
                    return Page(emptyList(), 0)
                }

                val lastRecord = records.last()
                val lastValue = lastRecord.getAtt(SORT_BY_ATT_ALIAS)
                val firstValue = records.first().getAtt(SORT_BY_ATT_ALIAS)
                if (this.lastValue == lastValue) {
                    skipCount += records.size
                } else if (firstValue == lastValue) {
                    skipCount += records.size
                } else {
                    // we add last element to page.skipCount to allow search by query (>= or <=) lastValue
                    skipCount = 1
                    for (idx in records.lastIndex - 1 downTo 0) {
                        val record = records[idx]
                        if (record.getAtt(SORT_BY_ATT_ALIAS) == lastValue) {
                            skipCount++
                        } else {
                            break
                        }
                    }
                }
                this.lastValue = lastValue

                if (lastValue.isNull() || lastValue.isTextual() && lastValue.asText().isEmpty()) {
                    log.warn {
                        "Last value of attribute '${pageSort.attribute}' is null or empty. " +
                            "Record: ${lastRecord.getId()}. Iteration will be stopped. Query: $query"
                    }
                    return Page(emptyList(), 0)
                } else {
                    page.records.forEach {
                        it.getAtts().remove(SORT_BY_ATT_ALIAS)
                    }
                }
            }
            return page
        }
    }

    private inner class AfterIdRecordsIterator : AbstractIterator() {

        override fun takeNextRecords(): Page {

            val query = baseQuery.copy()
                .withAfterId(RecordRef.valueOf(lastId))
                .withMaxItems(config.pageSize)
                .build()

            val page = queryRecords(query, config.attsToLoad)
            val records = page.records

            if (records.isNotEmpty()) {
                val lastRecord = records.last()
                val newLastId = lastRecord.getId()
                if (newLastId == lastId) {
                    return Page(emptyList(), 0)
                }
            }
            return page
        }
    }

    private abstract inner class AbstractIterator : RecordsIterator<RecordAtts> {

        private var currentIdx = 0
        private var records: List<RecordAtts> = emptyList()
        private var processedCount = 0L
        private var totalCount = -1L

        protected var lastId: EntityRef = EntityRef.EMPTY

        override fun getState(full: Boolean): ObjectData {

            val state = ObjectData.create()
                .set(STATE_BASE_PROCESSED_COUNT, processedCount)
                .set(STATE_BASE_TOTAL_COUNT, totalCount)
                .set(STATE_BASE_LAST_ID, lastId)

            if (full) {
                state[STATE_BASE_RECORDS] = records
                state[STATE_BASE_CURRENT_IDX] = currentIdx
            }

            return state
        }

        override fun setState(state: ObjectData) {
            currentIdx = state.get(STATE_BASE_CURRENT_IDX, 0)
            records = state[STATE_BASE_RECORDS].toList(RecordAtts::class.java)
            processedCount = state.get(STATE_BASE_PROCESSED_COUNT, 0L)
            totalCount = state.get(STATE_BASE_TOTAL_COUNT, -1L)
            lastId = state[STATE_BASE_LAST_ID].asText().toEntityRef()
        }

        protected abstract fun takeNextRecords(): Page

        override fun hasNext(): Boolean {
            val maxItems = baseQuery.page.maxItems
            if (maxItems in 1..processedCount) {
                return false
            }
            if (records.isEmpty() || currentIdx >= records.size) {
                val nextPage = takeNextRecords()
                records = nextPage.records
                if (totalCount == -1L) {
                    totalCount = nextPage.totalCount
                }
                currentIdx = 0
                if (records.isNotEmpty()) {
                    lastId = records.last().getId()
                }
            }
            return currentIdx < records.size
        }

        override fun next(): RecordAtts {
            val maxItems = baseQuery.page.maxItems
            if (maxItems in 1..processedCount) {
                throw NoSuchElementException()
            }
            processedCount++
            return records[currentIdx++]
        }

        override fun getProcessedCount(): Long {
            return processedCount
        }

        override fun getTotalCount(): Long {
            return totalCount.coerceAtLeast(processedCount)
        }
    }

    private class Page(
        val records: List<RecordAtts>,
        val totalCount: Long
    )
}
