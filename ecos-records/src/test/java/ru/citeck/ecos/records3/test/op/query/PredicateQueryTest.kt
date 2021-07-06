package ru.citeck.ecos.records3.test.op.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery

class PredicateQueryTest {

    @Test
    fun test() {

        val queryPredicates = mutableListOf<Predicate>()
        val dao = object : RecordsQueryDao {
            override fun getId() = "test"
            override fun queryRecords(recsQuery: RecordsQuery): Any? {
                queryPredicates.add(recsQuery.getQuery(Predicate::class.java))
                RecordsQuery
                return null
            }
        }

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(dao)
        val query = RecordsQuery.create {
            withSourceId("test")
            withLanguage(PredicateService.LANGUAGE_PREDICATE)
        }

        records.query(query.copy { withQuery(Predicates.eq("field", "value")) })
        assertThat(queryPredicates).containsExactly(Predicates.eq("field", "value"))
        queryPredicates.clear()

        val predTypes = listOf(ValuePredicate.Type.EQ, ValuePredicate.Type.CONTAINS, ValuePredicate.Type.LIKE)
        predTypes.forEach { predType ->
            val pred = ValuePredicate("someAtt", predType, listOf("value0", "value1"))
            records.query(query.copy { withQuery(pred) })
            assertThat(queryPredicates).containsExactly(
                Predicates.or(
                    ValuePredicate(pred.getAttribute(), predType, "value0"),
                    ValuePredicate(pred.getAttribute(), predType, "value1")
                )
            )
            queryPredicates.clear()
        }
    }
}
