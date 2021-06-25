package ru.citeck.ecos.records3.test.record.atts.value

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.RecordsServiceFactory

class AttValueTest {

    @Test
    fun test() {

        val record = ObjectData.create(
            """
            {
            "field0": {
                "field1": {
                    "num": 123,
                    "notNumStr": "aaa",
                    "numStr": "1234"
                }
            }
            }
            """.trimIndent()
        )

        val records = RecordsServiceFactory().recordsServiceV1

        val num = records.getAtt(record, "field0.field1.num?num").asDouble()
        assertThat(num).isEqualTo(123.0)

        val numStr = records.getAtt(record, "field0.field1.numStr?num").asDouble()
        assertThat(numStr).isEqualTo(1234.0)

        val notNumStr = records.getAtt(record, "field0.field1.notNumStr?num")
        assertThat(notNumStr.isNull()).isTrue()
        assertThat(notNumStr.asDouble()).isEqualTo(0.0)

        val allAtts = mapOf(
            "first" to "field0.field1.num?num",
            "second" to "field0.field1.numStr?num",
            "third" to "field0.field1.notNumStr?num"
        )
        val allAttsValue = records.getAtts(record, allAtts)

        assertThat(allAttsValue.getAtt("first").asDouble()).isEqualTo(123.0)
        assertThat(allAttsValue.getAtt("second").asDouble()).isEqualTo(1234.0)
        assertThat(allAttsValue.getAtt("third").isNull()).isTrue()
        assertThat(allAttsValue.getAtt("third").asDouble()).isEqualTo(0.0)
    }
}
