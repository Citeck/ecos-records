package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.factory.*
import ru.citeck.ecos.records3.record.atts.value.factory.time.DateValueFactory
import java.util.*

class ValuesConverterTest {

    @Test
    fun test() {

        val factory = RecordsServiceFactory()
        val conv = factory.attValuesConverter

        assertEquals(
            conv.toAttValue(true)!!::class.java,
            conv.getFactory(BooleanValueFactory::class.java).getValue(true)::class.java
        )
        assertEquals(
            conv.toAttValue(10)!!::class.java,
            conv.getFactory(IntegerValueFactory::class.java).getValue(10)::class.java
        )
        assertEquals(
            conv.toAttValue(10.0)!!::class.java,
            conv.getFactory(DoubleValueFactory::class.java).getValue(10.0)::class.java
        )
        assertEquals(
            conv.toAttValue(10.0f)!!::class.java,
            conv.getFactory(FloatValueFactory::class.java).getValue(10.0f)::class.java
        )
        assertEquals(
            conv.toAttValue(ByteArray(10) { it.toByte() })!!::class.java,
            conv.getFactory(ByteArrayValueFactory::class.java).getValue(ByteArray(10) { it.toByte() })::class.java
        )
        assertEquals(
            conv.toAttValue(Date())!!::class.java,
            conv.getFactory(DateValueFactory::class.java).getValue(Date())::class.java
        )
        assertEquals(
            conv.toAttValue(10L)!!::class.java,
            conv.getFactory(LongValueFactory::class.java)
                .getValue(10L)::class.java
        )
        assertEquals(
            conv.toAttValue(MLText())!!::class.java,
            conv.getFactory(MLTextValueFactory::class.java).getValue(MLText())!!::class.java
        )
        assertEquals(
            conv.toAttValue("12")!!::class.java,
            conv.getFactory(StringValueFactory::class.java).getValue("12")::class.java
        )
    }
}
