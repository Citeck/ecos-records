package ru.citeck.ecos.records3.test.predicate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.predicate.PredicateUtils
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

    @Test
    fun optimizationTest() {

        val optimizedEmptyAnd = PredicateUtils.optimize(Predicates.and())
        assertThat(optimizedEmptyAnd).isEqualTo(Predicates.alwaysTrue())

        val optimizedEmptyOr = PredicateUtils.optimize(Predicates.or())
        assertThat(optimizedEmptyOr).isEqualTo(Predicates.alwaysFalse())

        val eqPred = Predicates.eq("abc", "def")

        val optimizedAndWithTrue = PredicateUtils.optimize(
            Predicates.and(
                Predicates.alwaysTrue(),
                eqPred
            )
        )
        assertThat(optimizedAndWithTrue).isEqualTo(eqPred)

        val optimizedAndWithFalse = PredicateUtils.optimize(
            Predicates.and(
                Predicates.alwaysFalse(),
                eqPred
            )
        )
        assertThat(optimizedAndWithFalse).isEqualTo(Predicates.alwaysFalse())

        val optimizedOrWithTrue = PredicateUtils.optimize(
            Predicates.or(
                Predicates.alwaysTrue(),
                eqPred
            )
        )
        assertThat(optimizedOrWithTrue).isEqualTo(Predicates.alwaysTrue())

        val optimizedOrWithFalse = PredicateUtils.optimize(
            Predicates.or(
                Predicates.alwaysFalse(),
                eqPred
            )
        )
        assertThat(optimizedOrWithFalse).isEqualTo(eqPred)

        val notAlwaysTrue = PredicateUtils.optimize(Predicates.not(Predicates.alwaysTrue()))
        assertThat(notAlwaysTrue).isEqualTo(Predicates.alwaysFalse())
        val notAlwaysFalse = PredicateUtils.optimize(Predicates.not(Predicates.alwaysFalse()))
        assertThat(notAlwaysFalse).isEqualTo(Predicates.alwaysTrue())

        val notNotNot = PredicateUtils.optimize(
            Predicates.not(
                Predicates.not(
                    Predicates.not(
                        Predicates.not(
                            Predicates.not(
                                eqPred
                            )
                        )
                    )
                )
            )
        )
        assertThat(notNotNot).isEqualTo(Predicates.not(eqPred))

        val orWithNotAlwaysTrue = PredicateUtils.optimize(
            Predicates.or(
                Predicates.not(Predicates.alwaysTrue()),
                eqPred,
                Predicates.not(Predicates.alwaysTrue())
            )
        )
        assertThat(orWithNotAlwaysTrue).isEqualTo(eqPred)

        val orWithNotAlwaysFalse = PredicateUtils.optimize(
            Predicates.or(
                Predicates.not(Predicates.alwaysFalse()),
                eqPred,
                Predicates.not(Predicates.alwaysFalse())
            )
        )
        assertThat(orWithNotAlwaysFalse).isEqualTo(Predicates.alwaysTrue())

        val andWithNotAlwaysTrue = PredicateUtils.optimize(
            Predicates.and(
                Predicates.not(Predicates.alwaysTrue()),
                eqPred,
                Predicates.not(Predicates.alwaysTrue())
            )
        )
        assertThat(andWithNotAlwaysTrue).isEqualTo(Predicates.alwaysFalse())

        val andWithNotAlwaysFalse = PredicateUtils.optimize(
            Predicates.and(
                Predicates.not(Predicates.alwaysFalse()),
                eqPred,
                Predicates.not(Predicates.alwaysFalse())
            )
        )
        assertThat(andWithNotAlwaysFalse).isEqualTo(eqPred)
    }
}
