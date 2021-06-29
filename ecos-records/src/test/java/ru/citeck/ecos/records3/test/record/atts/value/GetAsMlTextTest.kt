package ru.citeck.ecos.records3.test.record.atts.value

import ecos.com.fasterxml.jackson210.databind.node.ObjectNode
import ecos.com.fasterxml.jackson210.databind.node.TextNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.RecordsServiceFactory
import java.util.*

class GetAsMlTextTest {

    @Test
    fun test() {

        val records = RecordsServiceFactory().recordsServiceV1
        val data = mapOf(
            "str" to "string",
            "obj" to ObjectData.create("""{"ru":"Ru","en":"En"}"""),
            "dataValue" to DataValue.create("""{"ru":"Ru","en":"En"}"""),
            "jsonNodeObj" to DataValue.create("""{"ru":"Ru","en":"En"}""").getAs(ObjectNode::class.java),
            "jsonNodeTxt" to DataValue.create("jsonNodeTxt").getAs(TextNode::class.java),
            "mltext" to MLText(Locale.ENGLISH to "En", Locale.FRANCE to "Fr")
        )

        val attsToTest = mapOf(
            "str._as.mltext" to "string",
            "str._as.mltext.closest.ru" to "string",
            "str._as.mltext.closest.en" to "string",
            "obj._as.mltext" to "En",
            "obj._as.mltext.ru" to "Ru",
            "obj._as.mltext.en" to "En",
            "dataValue._as.mltext" to "En",
            "dataValue._as.mltext.ru" to "Ru",
            "dataValue._as.mltext.en" to "En",
            "jsonNodeObj._as.mltext" to "En",
            "jsonNodeObj._as.mltext.ru" to "Ru",
            "jsonNodeObj._as.mltext.en" to "En",
            "mltext._as.mltext" to "En",
            "mltext._as.mltext.en" to "En",
            "mltext._as.mltext.fr_FR" to "Fr",
            "jsonNodeTxt._as.mltext" to "jsonNodeTxt",
            "jsonNodeTxt._as.mltext.closest.ru" to "jsonNodeTxt",
            "jsonNodeTxt._as.mltext.closest.en" to "jsonNodeTxt"
        )

        attsToTest.forEach { (att, expected) ->
            val actual = records.getAtt(data, att)
            assertThat(actual).withFailMessage {
                "att: $att actual: $actual expected: $expected"
            }.isEqualTo(DataValue.create(expected))
        }
    }
}
