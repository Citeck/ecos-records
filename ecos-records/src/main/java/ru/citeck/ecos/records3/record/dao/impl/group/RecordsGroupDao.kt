package ru.citeck.ecos.records3.record.dao.impl.group

import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.request.query.lang.DistinctQuery
import ru.citeck.ecos.records2.source.common.group.DistinctValue
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.op.query.dao.RecordsQueryDao
import ru.citeck.ecos.records3.record.op.query.dto.RecordsQuery
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import java.util.*

class RecordsGroupDao : AbstractRecordsDao(), RecordsQueryDao {

    override fun getId() = ID

    override fun queryRecords(query: RecordsQuery): RecsQueryRes<*> {

        val groupBy = query.groupBy
        if (groupBy.isEmpty()) {
            return RecsQueryRes<Any>()
        }
        val groupsBaseQuery = RecordsQuery(query)
        if (groupBy.size == 1) {
            groupsBaseQuery.groupBy = null
        } else {
            val newGroupBy: MutableList<String> = ArrayList()
            for (i in 1 until groupBy.size) {
                newGroupBy.add(groupBy[i])
            }
            groupsBaseQuery.groupBy = newGroupBy
        }
        val groupAtts = groupBy[0].split("&".toRegex()).toTypedArray()
        val max = if (query.maxItems > 0) query.maxItems else MAX_ITEMS_DEFAULT
        val basePredicate = query.getQuery(Predicate::class.java)
        val distinctValues: MutableList<List<DistinctValue>> = ArrayList()
        for (groupAtt in groupAtts) {
            val values = getDistinctValues(query.sourceId, basePredicate, groupAtt, max)
            if (values.isEmpty()) {
                return RecsQueryRes<Any>()
            }
            distinctValues.add(values)
        }
        val result = RecsQueryRes<RecordsGroup>()
        result.records = getGroups(groupsBaseQuery, distinctValues, basePredicate, groupAtts)
        return result
    }

    private fun getGroups(groupsBaseQuery: RecordsQuery,
                          distinctValues: List<List<DistinctValue>>,
                          basePredicate: Predicate,
                          attributes: Array<String>): List<RecordsGroup> {
        val groups: MutableList<RecordsGroup> = ArrayList()
        if (distinctValues.size == 1) {
            for (value in distinctValues[0]) {
                val attributesMap = Collections.singletonMap(attributes[0], value)
                groups.add(createGroup(groupsBaseQuery, attributesMap, basePredicate))
            }
        } else {
            for (value0 in distinctValues[0]) {
                for (value1 in distinctValues[1]) {
                    val attributesMap: MutableMap<String, DistinctValue> = HashMap()
                    attributesMap[attributes[0]] = value0
                    attributesMap[attributes[1]] = value1
                    groups.add(createGroup(groupsBaseQuery, attributesMap, basePredicate))
                }
            }
        }
        return groups
    }

    private fun createGroup(groupsBaseQuery: RecordsQuery,
                            attributes: Map<String, DistinctValue>,
                            basePredicate: Predicate): RecordsGroup {
        val groupQuery = RecordsQuery(groupsBaseQuery)
        val groupPredicate = Predicates.and()
        groupPredicate.addPredicate(basePredicate)
        attributes.forEach { (att: String?, `val`: DistinctValue) -> groupPredicate.addPredicate(Predicates.equal(att, `val`.value)) }
        groupQuery.query = groupPredicate
        groupQuery.language = PredicateService.LANGUAGE_PREDICATE
        return RecordsGroup(groupQuery, attributes, groupPredicate, recordsService!!)
    }

    private fun getDistinctValues(sourceId: String,
                                  predicate: Predicate,
                                  attribute: String,
                                  max: Int): List<DistinctValue> {

        val recordsQuery = RecordsQuery()
        recordsQuery.language = DistinctQuery.LANGUAGE
        val distinctQuery = DistinctQuery()
        distinctQuery.language = PredicateService.LANGUAGE_PREDICATE
        distinctQuery.query = predicate
        distinctQuery.attribute = attribute
        recordsQuery.maxItems = max
        recordsQuery.sourceId = sourceId
        recordsQuery.query = distinctQuery
        val values = recordsService!!.query(recordsQuery, DistinctValue::class.java)
        return values.records
    }

    override fun getSupportedLanguages(): List<String> {
        return listOf(PredicateService.LANGUAGE_PREDICATE)
    }

    companion object {
        const val ID = "group"
        private const val MAX_ITEMS_DEFAULT = 20
    }
}
