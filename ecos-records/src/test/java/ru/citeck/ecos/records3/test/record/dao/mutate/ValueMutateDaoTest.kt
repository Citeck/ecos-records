package ru.citeck.ecos.records3.test.record.dao.mutate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.mutate.ValueMutateDao

class ValueMutateDaoTest {

    @Test
    fun test() {

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(TestDao())

        val atts = records.mutate(
            "test@",
            mapOf(
                "argValue" to "someValue"
            ),
            listOf("respValue", "respWithPostfix", "unknown")
        )
        assertThat(atts.getAtt("respValue").asText()).isEqualTo("someValue")
        assertThat(atts.getAtt("respWithPostfix").asText()).isEqualTo("someValue-postfix")
        assertThat(atts.getAtt("unknown").isNull()).isTrue
    }

    class TestDao : ValueMutateDao<TestDao.ArgAtts> {

        override fun getId() = "test"

        override fun mutate(value: ArgAtts): Any {
            return RespAtts(value.argValue, value.argValue + "-postfix")
        }

        class ArgAtts(
            val argValue: String
        )

        class RespAtts(
            val respValue: String,
            val respWithPostfix: String
        )
    }
}
