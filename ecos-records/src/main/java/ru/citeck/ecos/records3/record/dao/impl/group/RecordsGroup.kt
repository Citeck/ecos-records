package ru.citeck.ecos.records3.record.dao.impl.group

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.predicate.model.ComposedPredicate
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.source.common.group.DistinctValue
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.impl.InnerAttValue
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import java.util.*

class RecordsGroup(
    private val query: RecordsQuery,
    attributes: Map<String, DistinctValue>,
    private val predicate: Predicate,
    private val recordsService: RecordsService
) : AttValue {

    companion object {
        const val FIELD_PREDICATE = "predicate"
        const val FIELD_PREDICATES = "predicates"
        const val FIELD_VALUES = "values"
        const val FIELD_SUM = "sum"
        const val FIELD_COUNT = "count"
    }

    private val attributes: MutableMap<String, ValueWrapper>

    init {
        this.attributes = LinkedHashMap()
        attributes.forEach { (n, v) ->
            this.attributes[n] = ValueWrapper(v)
        }
    }

    override fun asText(): String {
        return predicate.toString()
    }

    override fun getAtt(name: String): Any {
        when (name) {
            FIELD_PREDICATE -> return predicate
            FIELD_PREDICATES -> {
                return if (predicate is ComposedPredicate) {
                    predicate.getPredicates()
                } else {
                    listOf(predicate)
                }
            }
            FIELD_VALUES -> {
                val innerAttributes = AttContext.getInnerAttsMap()
                val records = recordsService.query(query, innerAttributes)
                return records.getRecords().map { recordAtts ->
                    val atts = recordAtts.getAtts()
                    atts.set("id", recordAtts.getId().toString())
                    InnerAttValue(Json.mapper.toJson(atts))
                }
            }
            else -> {
            }
        }
        if (name == FIELD_COUNT) {
            val countQuery = query.copy()
                .withGroupBy(emptyList())
                .withMaxItems(1)
                .withSkipCount(0)
                .build()
            val records = recordsService.query(countQuery)
            return records.getTotalCount()
        }
        if (name.startsWith(FIELD_SUM)) {
            val attribute = name.substring(FIELD_SUM.length + 1, name.length - 1) + "?num"
            val attributes = listOf(attribute)
            val sumQuery = query.copy().withGroupBy(emptyList()).build()
            val result = recordsService.query(sumQuery, attributes)
            var sum = 0.0
            for (record in result.getRecords()) {
                sum += record.getAtt(attribute, 0.0)
            }
            return sum
        }
        return attributes[name]!!
    }

    private class ValueWrapper internal constructor(private val value: DistinctValue) : AttValue {

        override fun asText(): String {
            return value.value
        }

        override fun getDisplayName(): String {
            return value.displayName
        }

        override fun getId(): Any? {
            return value.id
        }
    }
}
