package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.request.ctxatts.CtxAttsProvider
import kotlin.test.assertEquals

class DefaultCtxAttsProviderTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()
        services.ctxAttsService.register(object : CtxAttsProvider {
            override fun fillContextAtts(attributes: MutableMap<String, Any?>) {
                return attributes.putAll(
                    mapOf(
                        "test-str" to "value",
                        "test-num" to 123
                    )
                )
            }
        })
        val records = services.recordsServiceV1
        records.register(object : RecordAttsDao {
            override fun getRecordAtts(record: String): Any? = emptyMap<String, Any>()
            override fun getId(): String = "test"
        })

        val testStr = records.getAtt(RecordRef.valueOf("test@test"), "\$test-str").asText()
        assertEquals("value", testStr)

        val testNum = records.getAtt(RecordRef.valueOf("test@test"), "\$test-num?num").asDouble()
        assertEquals(123.0, testNum)
    }
}
