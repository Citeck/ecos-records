package ru.citeck.ecos.records3.test.predicate.comparator

import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records2.predicate.comparator.DefaultValueComparator
import java.time.Instant
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PredicateComparatorTest {

    @Test
    fun timeTest() {

        val comparator = DefaultValueComparator()
        val time = Instant.parse("2022-01-02T11:22:33Z")

        fun testWithValueConv(desc: String, firstArg: (Instant) -> Any, secondArg: (Instant) -> Any) {

            val firstArgDv: (Instant) -> DataValue = { DataValue.create(firstArg.invoke(it)) }
            val secondArgDv: (Instant) -> DataValue = { DataValue.create(secondArg.invoke(it)) }

            assertTrue(comparator.isEquals(firstArgDv(time), secondArgDv(time)), desc)
            assertTrue(comparator.isGreaterThan(firstArgDv(time), secondArgDv(time), true), desc)
            assertTrue(comparator.isLessThan(firstArgDv(time), secondArgDv(time), true), desc)
            assertFalse(comparator.isGreaterThan(firstArgDv(time), secondArgDv(time), false), desc)
            assertFalse(comparator.isLessThan(firstArgDv(time), secondArgDv(time), false), desc)

            val tenSecondsBefore = time.minusSeconds(10)
            assertTrue(comparator.isGreaterThan(firstArgDv(time), secondArgDv(tenSecondsBefore), false), desc)
            assertTrue(comparator.isLessThan(firstArgDv(tenSecondsBefore), secondArgDv(time), false), desc)
        }

        testWithValueConv("time:toString-toString", { it.toString() }, { it.toString() })
        testWithValueConv("time:toString-toMillis", { it.toString() }, { it.toEpochMilli() })
        testWithValueConv("time:toMillis-toMillis", { it.toEpochMilli() }, { it.toEpochMilli() })
        testWithValueConv("time:toMillis-toString", { it.toEpochMilli() }, { it.toString() })
    }

    @Test
    fun numAndStrTest() {

        val comparator = DefaultValueComparator()
        val valueNum = 10

        fun testWithValueConv(desc: String, firstArg: (Int) -> Any, secondArg: (Int) -> Any) {

            val firstArgDv: (Int) -> DataValue = { DataValue.create(firstArg.invoke(it)) }
            val secondArgDv: (Int) -> DataValue = { DataValue.create(secondArg.invoke(it)) }

            assertTrue(comparator.isEquals(firstArgDv(valueNum), secondArgDv(valueNum)), desc)
            assertTrue(comparator.isGreaterThan(firstArgDv(valueNum), secondArgDv(valueNum), true), desc)
            assertTrue(comparator.isLessThan(firstArgDv(valueNum), secondArgDv(valueNum), true), desc)
            assertFalse(comparator.isGreaterThan(firstArgDv(valueNum), secondArgDv(valueNum), false), desc)
            assertFalse(comparator.isLessThan(firstArgDv(valueNum), secondArgDv(valueNum), false), desc)

            val valueNumMinus5 = valueNum - 5
            assertTrue(comparator.isGreaterThan(firstArgDv(valueNum), secondArgDv(valueNumMinus5), false), desc)
            assertTrue(comparator.isLessThan(firstArgDv(valueNumMinus5), secondArgDv(valueNum), false), desc)
        }

        testWithValueConv("num:toString-toString", { it.toString() }, { it.toString() })
        testWithValueConv("num:toString-num", { it.toString() }, { it })
        testWithValueConv("num:num-toString", { it }, { it.toString() })
        testWithValueConv("num:num-num", { it }, { it })
    }

    @Test
    fun objTest() {

        val comparator = DefaultValueComparator()

        val obj = DataValue.createObj()
            .set("ru", "Ru name")
            .set("en", "En name")

        assertTrue(comparator.isContains(obj, DataValue.create("name")))
        assertTrue(comparator.isContains(obj, DataValue.create("Name")))
        assertTrue(comparator.isEquals(obj, DataValue.create("Ru name")))
    }
}
