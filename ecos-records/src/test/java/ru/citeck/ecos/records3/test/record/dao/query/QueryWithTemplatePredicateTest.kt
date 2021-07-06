package ru.citeck.ecos.records3.test.record.dao.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.mixin.AttMixin

class QueryWithTemplatePredicateTest {

    @Test
    fun test() {

        val records = RecordsServiceFactory().recordsServiceV1

        val queries = mutableListOf<Predicate>()
        records.register(object : RecordsQueryDao {
            override fun getId() = "test"
            override fun queryRecords(recsQuery: RecordsQuery): Any? {
                if (recsQuery.language == PredicateService.LANGUAGE_PREDICATE) {
                    queries.add(recsQuery.getQuery(Predicate::class.java))
                }
                return null
            }
        })

        val mixinValue = "mixin-value"
        val predValue = DataValue.create("""{"t":"gt","a":"sum","v":1000}""").getAs(Predicate::class.java)

        records.getRecordsDao("meta", AbstractRecordsDao::class.java)!!.addAttributesMixin(object : AttMixin {
            override fun getAtt(path: String, value: AttValueCtx): Any? {
                if (path == "att") {
                    return mixinValue
                } else if (path == "pred") {
                    return predValue
                }
                return null
            }
            override fun getProvidedAtts(): Collection<String> = listOf("att", "pred")
        })

        queries.clear()
        records.query(
            RecordsQuery.create {
                withSourceId("test")
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery("""{"t":"and","v":"${"\${pred[]?json}"}"}""")
            }
        )

        assertThat(queries).containsExactly(PredicateUtils.optimize(Predicates.and(predValue)))

        val testWithSingleAtt = { att: String, expected: DataValue ->
            queries.clear()
            records.query(
                RecordsQuery.create {
                    withSourceId("test")
                    withLanguage(PredicateService.LANGUAGE_PREDICATE)
                    withQuery(Predicates.eq("att", att))
                }
            )
            assertThat(queries).containsExactly(Predicates.eq("att", expected))
        }

        testWithSingleAtt("\${att}", DataValue.createStr(mixinValue))
        testWithSingleAtt("prefix-\${att}", DataValue.create("prefix-$mixinValue"))
        testWithSingleAtt("\${att}-postfix", DataValue.create("$mixinValue-postfix"))
        testWithSingleAtt("prefix-\${att}-postfix", DataValue.create("prefix-$mixinValue-postfix"))

        testWithSingleAtt("\${unknown}", DataValue.NULL)
        testWithSingleAtt("\${unknown!}", DataValue.createStr(""))
        testWithSingleAtt("\${unknown!'or-else'}", DataValue.createStr("or-else"))
        testWithSingleAtt("\${unknown!false}", DataValue.create(false))
    }
}
