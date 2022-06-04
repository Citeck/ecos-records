package ru.citeck.ecos.records3.test.predicate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsServiceFactory

class PredicateFilterTest {

    @Test
    fun test() {

        val predicateService = RecordsServiceFactory().predicateService

        val elements = listOf(
            mapOf(
                "aa" to "bb",
                "num" to 10,
                "ref" to RecordRef.create("app", "src", "id")
            )
        )

        fun testTruePredicate(pred: Predicate) {
            val res = predicateService.filter(elements, pred)
            assertThat(res).hasSize(1)
            assertThat(res[0]).isEqualTo(elements[0])
        }

        testTruePredicate(Predicates.eq("num", 10.0))
        testTruePredicate(Predicates.ge("num", 10.0))
        testTruePredicate(Predicates.le("num", 10.0))
        testTruePredicate(Predicates.gt("num", 9.0))
        testTruePredicate(Predicates.lt("num", 11.0))

        testTruePredicate(Predicates.eq("ref", "app/src@id"))
    }
}
