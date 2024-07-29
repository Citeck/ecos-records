package ru.citeck.ecos.records3.test.bean

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault
import ru.citeck.ecos.records3.RecordsServiceFactory

class BeanValueFactoryGetAsTextTest {

    @Test
    fun getAsTextTest() {

        val records = RecordsServiceFactory().recordsService

/*        val dto0 = TestDataDtoWithoutToString(field1 = 12345)
        val value0 = records.getAtt(dto0, "?str").asText()
        assertThat(value0).isEqualTo("{\"field1\":12345}")
        val value1 = records.getAtt(dto0, "?disp").asText()
        assertThat(value1).isEqualTo(value0)

        val dto1 = TestDtoWithoutToString(field1 = 123456)
        val value2 = records.getAtt(dto1, "?str").asText()
        assertThat(value2).isEqualTo("{\"field1\":123456}")
        val value3 = records.getAtt(dto1, "?disp").asText()
        assertThat(value3).isEqualTo(value2)

        val dto2 = TestDtoWithToString(field1 = 123456789)
        val value4 = records.getAtt(dto2, "?str").asText()
        assertThat(value4).isEqualTo("{\"field1\":123456789}")
        val value5 = records.getAtt(dto2, "?disp").asText()
        assertThat(value5).isEqualTo(value4)

        val mapValue = mapOf(
            "aa" to "bb",
            "cc" to 123
        )
        val mapStrValue = records.getAtt(mapValue, "?str").asText()
        assertThat(mapStrValue).isEqualTo("{\"aa\":\"bb\",\"cc\":123}")
        val mapDispValue = records.getAtt(mapValue, "?disp").asText()
        assertThat(mapDispValue).isEqualTo(mapStrValue)

        val customJsonDto = TestDtoWithCustomJsonView(field0 = "abc")
        val customJsonValue = records.getAtt(customJsonDto, "?str").asText()
        assertThat(customJsonValue).isEqualTo("{\"custom\":\"json\"}")
        val customJsonValue2 = records.getAtt(customJsonDto, "?disp").asText()
        assertThat(customJsonValue2).isEqualTo(customJsonValue)*/

        val customAsTextDto = TestDtoWithCustomTextView(field0 = "abc")
        val customTextValue = records.getAtt(customAsTextDto, "?str").asText()
        assertThat(customTextValue).isEqualTo("abc")
        val customTextValue2 = records.getAtt(customAsTextDto, "?disp").asText()
        assertThat(customTextValue2).isEqualTo(customTextValue)
    }

    @IncludeNonDefault
    data class TestDataDtoWithoutToString(
        val field0: String = "",
        val field1: Int = 123
    )

    @IncludeNonDefault
    class TestDtoWithoutToString(
        val field0: String = "",
        val field1: Int = 123
    )

    @IncludeNonDefault
    class TestDtoWithToString(
        val field0: String = "",
        val field1: Int = 123
    ) {
        override fun toString(): String {
            return "custom-to-string"
        }
    }

    class TestDtoWithCustomJsonView(
        val field0: String = "",
        val field1: Int = 123
    ) {
        fun getAsJson(): DataValue {
            return DataValue.create("{\"custom\":\"json\"}")
        }
    }

    class TestDtoWithCustomTextView(
        val field0: String = "",
        val field1: Int = 123
    ) {
        fun getAsStr(): String {
            return "abc"
        }
    }
}
