package ru.citeck.ecos.records3.test.predicate

import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsServiceFactory
import kotlin.test.assertEquals

class PredicateTest2 {

    @Test
    fun simpleNotationTest() {

        RecordsServiceFactory()

        val pred = """
            {
                "t": "and",
                "v": [
                    {
                        "t": "eq",
                        "a": "attribute",
                        "v": 123
                    },
                    {
                        "t": "not-gt",
                        "a": "num",
                        "v": 1234.0
                    },
                    {
                        "t": "and",
                        "v": [
                            {
                                "t": "empty",
                                "a": "empty-att"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val expected = Predicates.and(
            Predicates.eq("attribute", 123),
            Predicates.not(
                Predicates.gt("num", 1234.0)
            ),
            Predicates.and(
                Predicates.empty("empty-att")
            )
        )
        val actual = Json.mapper.read(pred, Predicate::class.java)
        assertEquals(expected, actual)

        val str = Json.mapper.toString(actual)!!
        assertEquals(str.indexOf("\"a\""), -1)
        assertEquals(str.indexOf("\"v\""), -1)
    }
}
