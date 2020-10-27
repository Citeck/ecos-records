package ru.citeck.ecos.records3.record.dao.impl.group

import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.records2.predicate.model.ComposedPredicate
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.source.common.group.DistinctValue
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.op.atts.service.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.InnerAttValue
import ru.citeck.ecos.records3.record.op.query.dto.RecordsQuery
import java.util.*
import java.util.stream.Collectors

class RecordsGroup(private val query: RecordsQuery,
                   attributes: Map<String, DistinctValue>,
                   private val predicate: Predicate,
                   private val recordsService: RecordsService) : AttValue {

    companion object {
        const val FIELD_PREDICATE = "predicate"
        const val FIELD_PREDICATES = "predicates"
        const val FIELD_VALUES = "values"
        const val FIELD_SUM = "sum"
        const val FIELD_COUNT = "count"
    }

    private val attributes: MutableMap<String, ValueWrapper>

    init {
        this.attributes = HashMap()
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
                    predicate.predicates
                } else listOf(predicate)
            }
            FIELD_VALUES -> {
                val innerAttributes = AttContext.getInnerAttsMap()
                val records = recordsService.query(query, innerAttributes)
                return records.records.stream().map { r: RecordAtts ->
                    val atts = r.getAtts()
                    atts.set("id", r.getId().toString())
                    InnerAttValue(mapper.toJson(atts))
                }.collect(Collectors.toList())
            }
            else -> {
            }
        }
        if (name == FIELD_COUNT) {
            val countQuery = RecordsQuery(query)
            countQuery.groupBy = null
            countQuery.maxItems = 1
            countQuery.skipCount = 0
            val records = recordsService.query(countQuery)
            return records.totalCount
        }
        if (name.startsWith(FIELD_SUM)) {
            val attribute = name.substring(FIELD_SUM.length + 1, name.length - 1) + "?num"
            val attributes = listOf(attribute)
            val sumQuery = RecordsQuery(query)
            sumQuery.groupBy = null
            val result = recordsService.query(sumQuery, attributes)
            var sum = 0.0
            for (record in result.records) {
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

        override fun getDispName(): String {
            return value.displayName
        }

        override fun getId(): String {
            return value.id
        }
    }
}
