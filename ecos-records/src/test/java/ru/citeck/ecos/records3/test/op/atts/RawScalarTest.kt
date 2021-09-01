package ru.citeck.ecos.records3.test.op.atts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.RecordsServiceFactory

class RawScalarTest {

    @Test
    fun test() {

        val data = ObjectData.create()
        data.set("int", 123)
        data.set("double", 123.0)
        data.set("str", "123.0")
        data.set("bool", true)

        val objValue = DataValue.create("""{"key": "value"}""")
        assertThat(objValue.get("key").asText()).isEqualTo("value")
        data.set("obj", objValue)

        val arrValue = DataValue.create("""["one", "two", "three"]""")
        assertThat(arrValue.get(1).asText()).isEqualTo("two")
        data.set("arr", arrValue)

        val records = RecordsServiceFactory().recordsServiceV1

        val int = records.getAtt(data, "int?raw")
        assertThat(int.isInt()).isTrue()
        assertThat(int.asInt()).isEqualTo(123)

        val double = records.getAtt(data, "double?raw")
        assertThat(double.isDouble()).isTrue()
        assertThat(double.asDouble()).isEqualTo(123.0)

        val str = records.getAtt(data, "str?raw")
        assertThat(str.isTextual()).isTrue()
        assertThat(str.asText()).isEqualTo("123.0")

        val bool = records.getAtt(data, "bool?raw")
        assertThat(bool.isBoolean()).isTrue()
        assertThat(bool.asBoolean()).isEqualTo(true)

        val arr = records.getAtt(data, "arr[]?raw")
        assertThat(arr).isEqualTo(arrValue)

        val obj = records.getAtt(data, "obj?raw")
        assertThat(obj).isEqualTo(objValue)
    }
}
