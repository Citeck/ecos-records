package ru.citeck.ecos.records3.test.record.atts.value.factory

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records3.RecordsServiceFactory

class DataValueFactoryTest {

    @Test
    fun test() {

        val data = DataValue.createObj()
            .set("numStr", "123")
            .set("numNum", 123)
            .set("boolStrTrue", "true")
            .set("boolStrFalse", "false")
            .set("boolBoolTrue", true)
            .set("boolBoolFalse", false)
            .set("ref", "uiserv/action@123")
            .set("id", "id-value")
            .set(
                "arr",
                DataValue.createArr()
                    .add("first")
                    .add("second")
                    .add("third")
            )

        val records = RecordsServiceFactory().recordsService
        val getAtt = { att: String -> records.getAtt(data, att).asJavaObj() }

        assertThat(getAtt("numStr?num")).isEqualTo(123.0)
        assertThat(getAtt("numNum?num")).isEqualTo(123.0)
        assertThat(getAtt("boolStrTrue?bool")).isEqualTo(true)
        assertThat(getAtt("boolStrFalse?bool")).isEqualTo(false)
        assertThat(getAtt("boolBoolTrue?bool")).isEqualTo(true)
        assertThat(getAtt("boolBoolFalse?bool")).isEqualTo(false)
        assertThat(getAtt("id")).isEqualTo("id-value")

        assertThat(DataValue.create(getAtt("?json"))).isEqualTo(data)
        assertThat(DataValue.create(getAtt("?raw"))).isEqualTo(data)

        assertThat(getAtt("_has.boolStrTrue?bool")).isEqualTo(true)
        assertThat(getAtt("_has.boolStrTrueUnknown?bool")).isEqualTo(false)

        assertThat(getAtt("ref._as.ref?localId")).isEqualTo("123")

        assertThat(getAtt("arr[]._as.ref?localId") as List<*>).containsExactly("first", "second", "third")
    }
}
