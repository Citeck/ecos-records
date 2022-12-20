package ru.citeck.ecos.records3.test.record.atts.value.factory.custom

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records3.RecordsServiceFactory

class CustomAttValueFactoryTest {

    @Test
    fun test() {

        val records = RecordsServiceFactory().recordsServiceV1
        val customDto = CustomDto(DataValue.createObj()
            .set("field0", "value0")
            .set("field1", 123))

        assertThat(records.getAtt(customDto, "field0").asText()).isEqualTo("value0")
        assertThat(records.getAtt(customDto, "field1").asInt()).isEqualTo(123)
    }
}
