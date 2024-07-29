package ru.citeck.ecos.records3.test.bean

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault
import ru.citeck.ecos.records3.RecordsServiceFactory

class BeanValueFactoryTest3 {

    @Test
    fun nonDefaultJsonTest() {

        val records = RecordsServiceFactory().recordsService

        val dto0 = TestDto(field1 = 12345)
        val value0 = records.getAtt(dto0, "?json")
        assertThat(value0.size()).isEqualTo(1)
        assertThat(value0.get("field1").asInt()).isEqualTo(12345)

        val dto1 = TestDto(field0 = "12345")
        val value1 = records.getAtt(dto1, "?json")
        assertThat(value1.size()).isEqualTo(1)
        assertThat(value1.get("field0").asText()).isEqualTo("12345")

        val dto2 = TestDto(field0 = "12345", field1 = 12345)
        val value2 = records.getAtt(dto2, "?json")
        assertThat(value2.size()).isEqualTo(2)
        assertThat(value2.get("field0").asText()).isEqualTo("12345")
        assertThat(value2.get("field1").asInt()).isEqualTo(12345)
    }

    @IncludeNonDefault
    data class TestDto(
        val field0: String = "",
        val field1: Int = 123
    )
}
