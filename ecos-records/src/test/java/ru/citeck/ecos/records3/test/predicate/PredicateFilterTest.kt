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
    fun testWithNull(ascending: Boolean) {

        val predicateService = RecordsServiceFactory().predicateService
        val strElements = listOf(
            "2020-10-08T04:10:37.020Z", "2020-06-30T12:30:00.972Z", "2022-09-06T08:13:17.449Z",
            "2020-06-30T12:30:00.821Z", "2020-10-08T04:10:37.360Z", "2020-06-30T12:30:00.260Z",
            "2020-10-28T22:39:45.693Z", "2020-06-30T12:30:00.891Z", "2021-10-11T08:05:54.210Z",
            "2020-06-30T12:30:00.398Z", "2020-06-30T12:30:00.125Z", "2020-06-30T12:30:00.519Z",
            "2022-10-21T14:12:39.192Z", "2022-09-21T07:40:27.231Z", "2020-08-26T05:02:48.725Z",
            "2022-05-18T12:50:28.583Z", "2022-04-14T13:32:13.251Z", "2020-10-08T04:10:47.472Z",
            "2022-04-14T13:32:13.209Z", "2022-04-14T13:32:13.072Z", "2020-06-30T12:29:59.369Z",
            "2022-01-12T08:20:29.627Z", "2022-11-01T19:15:45.038Z", "2021-05-05T06:45:32.736Z",
            "2020-11-11T06:59:48.172Z", "2020-10-08T04:10:35.829Z", "2020-10-08T04:10:35.691Z",
            "2020-10-08T04:10:35.270Z", "2020-11-11T06:59:47.887Z", "2021-03-03T21:14:33.456Z",
            "2021-03-03T21:14:33.389Z", "2021-11-23T20:11:58.345Z", "2022-03-09T05:37:19.438Z",
            "2021-02-23T09:35:51.735Z", "2020-05-20T00:00:00Z", "2020-05-20T00:00:00Z",
            "2021-02-23T09:35:51.622Z", "2020-05-20T00:00:00Z", "2021-02-23T09:35:50.455Z",
            "2021-02-23T09:35:49.961Z", "2020-05-20T00:00:00Z", "2021-11-18T07:14:56.666Z",
            "2021-06-04T08:34:13.496Z", "2020-05-20T00:00:00Z", "2020-05-20T00:00:00Z",
            "2021-02-23T09:35:49.881Z", "2020-05-20T00:00:00Z", "2020-05-20T00:00:00Z",
            "2020-05-20T00:00:00Z", "2020-11-27T22:40:39.332Z", "2020-05-20T00:00:00Z",
            "2021-02-23T09:35:50.971Z", "2021-11-18T07:14:56.439Z", "2021-02-23T09:35:50.379Z",
            null, "2021-02-23T09:35:52.218Z", "2020-05-20T00:00:00Z", "2020-05-20T00:00:00Z",
            "2021-04-12T15:05:12.475Z", "2021-04-09T12:27:25.742Z", "2020-05-20T00:00:00Z",
            "2021-04-16T15:15:38.275Z", "2020-05-20T00:00:00Z", "2020-05-20T00:00:00Z",
            "2020-05-20T00:00:00Z", "2020-05-20T00:00:00Z", "2020-05-20T00:00:00Z",
            "2020-05-20T00:00:00Z", "2020-05-20T00:00:00Z", "2022-08-08T15:50:40.065Z",
            "2020-05-20T00:00:00Z", "2020-06-30T12:32:48.923Z"
        ).map { NullableAnyValue(it) }

        val sortedElements = if (ascending) {
            strElements.sortedBy { it.value as? String ?: "" }
        } else {
            strElements.sortedByDescending { it.value as? String ?: "" }
        }

        val strSortRes = predicateService.filterAndSort(
            strElements,
            Predicates.alwaysTrue(),
            listOf(SortBy("value", ascending)),
            0,
            1000
        )

        assertThat(strSortRes).containsExactlyElementsOf(sortedElements)
    }

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

    data class NullableAnyValue(val value: Any?)
}
