package ru.citeck.ecos.records3.test.predicate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsServiceFactory

class RawPredicateTest {

    @Test
    fun test() {

        val elements = listOf(
            DtoValue("one", 0),
            DtoValue("two", 1),
            DtoValue("three", 2),
            DtoValue("four", 3),
            DtoValue("five", 4),
            DtoValue("six", 5),
            DtoValue("seven", 6)
        )

        val factory = RecordsServiceFactory()
        val predicateService = factory.predicateService

        assertEquals(
            elements.filter { it.strField == "one" },
            predicateService.filter(elements, Predicates.eq("strField", "one"))
        )
        assertEquals(
            elements.filter { it.intField > 3 },
            predicateService.filter(elements, Predicates.gt("intField", 3.0))
        )
        assertEquals(
            elements.filter { it.intField >= 3 },
            predicateService.filter(elements, Predicates.ge("intField", 3.0))
        )
    }

    data class DtoValue(
        val strField: String,
        val intField: Int
    )
}
