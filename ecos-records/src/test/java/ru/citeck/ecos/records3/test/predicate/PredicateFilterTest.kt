package ru.citeck.ecos.records3.test.predicate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy

class PredicateFilterTest {

    @ValueSource(strings = ["true", "false"])
    @ParameterizedTest
    fun filterAndSortTest(ascending: Boolean) {

        val predicateService = RecordsServiceFactory().predicateService

        val values = listOf(
            "AAB", "eeeeeeeeeee", "a", "abc", "defhig"
        ).map { StrValue(it) }

        val sortRes = predicateService.filterAndSort(
            values,
            Predicates.alwaysTrue(),
            listOf(SortBy("value", ascending)),
            0,
            1000
        )
        var expected = values.map { it.value }
        expected = if (ascending) {
            expected.sorted()
        } else {
            expected.sortedDescending()
        }
        assertThat(sortRes.map { it.value }).containsExactlyElementsOf(expected)
    }

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
            assertThat(res).describedAs(pred.toString()).hasSize(1)
            assertThat(res[0]).describedAs(pred.toString()).isEqualTo(elements[0])

            assertTrue(predicateService.isMatch(elements[0], pred)) { pred.toString() }
        }

        testTruePredicate(Predicates.eq("num", 10.0))
        testTruePredicate(Predicates.ge("num", 10.0))
        testTruePredicate(Predicates.le("num", 10.0))
        testTruePredicate(Predicates.gt("num", 9.0))
        testTruePredicate(Predicates.lt("num", 11.0))

        testTruePredicate(Predicates.eq("ref", "app/src@id"))
    }

    data class StrValue(val value: String)
}
