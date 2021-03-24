package ru.citeck.ecos.records3.test.record.atts.proc

import ru.citeck.ecos.records3.RecordsServiceFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class AttRegexpGroupProcessorTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1

        val test0 = TestDto("testField\$abcd")

        val res0 = records.getAtt(test0, "field|rxg('.+\\\$(.+)')").asText()
        assertEquals("abcd", res0)

        val res1 = records.getAtt(test0, "field|rxg('(.+)\\\$(.+)')").asText()
        assertEquals("testField", res1)

        val res2 = records.getAtt(test0, "field|rxg('(.+)\\\$(.+)', 2)").asText()
        assertEquals("abcd", res2)

        val res3 = records.getAtt(test0, "field|rxg('(.+)\\\$(.+)', 3)").asText()
        assertEquals("", res3)

        val res4 = records.getAtt(
            TestDto("et-status://emodel/type@type-ecos-fin-request/draft"),
            "field|rxg('.+/(.+)')").asText()
        assertEquals("draft", res4)
    }

    class TestDto(
        val field: String
    )
}
