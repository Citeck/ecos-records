package ru.citeck.ecos.records3.test.predicate

import lombok.Data
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.PredicateUtils.convertToDto
import ru.citeck.ecos.records2.predicate.PredicateUtils.getAllPredicateAttributes
import ru.citeck.ecos.records2.predicate.PredicateUtils.mapAttributePredicates
import ru.citeck.ecos.records2.predicate.model.*
import ru.citeck.ecos.records2.predicate.model.Predicates.alwaysFalse
import ru.citeck.ecos.records2.predicate.model.Predicates.alwaysTrue
import ru.citeck.ecos.records2.predicate.model.Predicates.and
import ru.citeck.ecos.records2.predicate.model.Predicates.empty
import ru.citeck.ecos.records2.predicate.model.Predicates.eq
import ru.citeck.ecos.records2.predicate.model.Predicates.ge
import ru.citeck.ecos.records2.predicate.model.Predicates.gt
import ru.citeck.ecos.records2.predicate.model.Predicates.le
import ru.citeck.ecos.records2.predicate.model.Predicates.lt
import ru.citeck.ecos.records2.predicate.model.Predicates.or

class PredicateUtilsTest {

    companion object {
        @JvmStatic
        fun getCompositePredicates(): List<Predicate> {
            return listOf(
                AndPredicate(),
                OrPredicate()
            )
        }
    }

    @ParameterizedTest
    @MethodSource("getCompositePredicates")
    fun testWithEmptyCompositePredicate(predicate: Predicate?) {
        val result = mapAttributePredicates(
            predicate,
            { it: AttributePredicate? -> it },
            onlyAnd = false,
            optimize = true,
            filterEmptyComposite = false
        )
        if (predicate is AndPredicate) {
            assertThat(result).isEqualTo(alwaysTrue())
        } else {
            assertThat(result).isEqualTo(alwaysFalse())
        }
    }

    @Test
    fun compositePredicateOptimizationTest() {
        val orRes = PredicateUtils.mapValuePredicates(
            or(
                eq("aa", "bb"),
                eq("cc", "dd")
            ),
            { alwaysFalse() },
            onlyAnd = false,
            optimize = true,
            filterEmptyComposite = false
        )
        assertThat(orRes).isEqualTo(alwaysFalse())

        val andRes = PredicateUtils.mapValuePredicates(
            and(
                eq("aa", "bb"),
                eq("cc", "dd")
            ),
            { alwaysTrue() },
            onlyAnd = false,
            optimize = true,
            filterEmptyComposite = false
        )
        assertThat(andRes).isEqualTo(alwaysTrue())
    }

    @Test
    fun test() {
        val field0Value = "test-field0"
        val field1Value = 123
        val field2Value = 123.123
        val field22Value = 123.12322

        val fieldWithPrefixValue = "fieldWithPrefixValue"

        val otherField = "OtherField"
        val otherValue = "OtherValue"
        val emptyValue = ""

        val pred: Predicate = and(
            eq("field0", field0Value),
            eq("field1", "" + field1Value),
            and(
                eq("field2", "" + field2Value),
                eq("field22", "" + field22Value)
            ),
            or(
                eq("field1", "" + field1Value + "123"),
                eq("field3", "" + field1Value + "123")
            ),
            eq(otherField, otherValue),
            eq("__fieldWithPrefix", fieldWithPrefixValue)
        )

        var dto = convertToDto(pred, PredDto::class.java)

        assertThat(dto.field0).isEqualTo(field0Value)
        assertThat(dto.field1).isEqualTo(field1Value)
        assertThat(dto.field2).isEqualTo(field2Value)
        assertThat(dto.field22).isEqualTo(field22Value)
        assertThat(dto.fieldWithPrefix).isEqualTo(fieldWithPrefixValue)
        assertThat(dto.field3).isEqualTo(123123.0)
        assertThat(dto.field4).isNull()

        assertThat(dto.predicate).isEqualTo(eq(otherField, otherValue))

        dto = convertToDto(
            eq("fieldWithPrefix", fieldWithPrefixValue),
            PredDto::class.java
        )

        assertThat(dto.fieldWithPrefix).isEqualTo(fieldWithPrefixValue)

        dto = convertToDto(pred, PredDto::class.java, true)
        assertThat(dto.field3).isNull()

        val dto2 = convertToDto(pred, DtoWithoutPredicateField::class.java)
        assertThat(dto2.field0).isEqualTo(dto.field0)
    }

    @Test
    fun attsTest() {
        val predicate: Predicate = and(
            empty("emptyField0"),
            and(
                eq("field0", "value"),
                le("field1", 123.0),
                lt("field2", 123.0),
                ge("field3", 123.0),
                gt("field4", 123.0),
                empty("emptyField1")
            )
        )

        val atts = getAllPredicateAttributes(predicate)
        assertThat(atts).containsExactlyInAnyOrder(
            "emptyField0",
            "emptyField1",
            "field0",
            "field1",
            "field2",
            "field3",
            "field4"
        )
    }

    @Data
    class PredDto {
        var field0: String? = null
        var field1: Int = 0
        var field2: Double? = null
        var field22: Double? = null
        var field3: Double? = null
        var field4: String? = null

        var predicate: Predicate? = null

        var fieldWithPrefix: String? = null

        fun set__fieldWithPrefix(value: String?) {
            fieldWithPrefix = value
        }
    }

    @Data
    class DtoWithoutPredicateField {
        var field0: String? = null
    }
}
