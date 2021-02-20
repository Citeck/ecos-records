package ru.citeck.ecos.records3.test.op.atts

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.factory.*
import ru.citeck.ecos.records3.record.atts.value.factory.time.DateValueFactory
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ValuesConverterTest {

    @Test
    fun test() {

        val factory = RecordsServiceFactory()

        assertEquals(
            factory.attValuesConverter.toAttValue(true)!!::class.java,
            BooleanValueFactory().getValue(true)!!::class.java
        )
        assertEquals(
            factory.attValuesConverter.toAttValue(10)!!::class.java,
            IntegerValueFactory().getValue(10)!!::class.java
        )
        assertEquals(
            factory.attValuesConverter.toAttValue(10.0)!!::class.java,
            DoubleValueFactory().getValue(10.0)!!::class.java
        )
        assertEquals(
            factory.attValuesConverter.toAttValue(ByteArray(10) { it.toByte() })!!::class.java,
            ByteArrayValueFactory().getValue(ByteArray(10) { it.toByte() })!!::class.java
        )
        assertEquals(
            factory.attValuesConverter.toAttValue(Date())!!::class.java,
            DateValueFactory().getValue(Date())!!::class.java
        )
        assertEquals(
            factory.attValuesConverter.toAttValue(10L)!!::class.java,
            LongValueFactory().getValue(10L)!!::class.java
        )
        assertEquals(
            factory.attValuesConverter.toAttValue(MLText())!!::class.java,
            MLTextValueFactory().getValue(MLText())!!::class.java
        )
        assertEquals(
            factory.attValuesConverter.toAttValue("12")!!::class.java,
            StringValueFactory().getValue("12")!!::class.java
        )
    }
}
