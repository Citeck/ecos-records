package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.op.atts.dao.RecordAttsDao
import kotlin.test.assertEquals

class RecordRefAttTest {

    companion object {
        val testRecordRef = RecordRef.create("test-app", "sourceId", "test")
    }

    @Test
    fun test() {

        val services = RecordsServiceFactory()

        services.recordsServiceV1.register(object : RecordAttsDao {
            override fun getRecordAtts(record: String): Any? {
                return ObjectData.create()
            }
            override fun getId() = testRecordRef.sourceId
        })

        assertEquals(testRecordRef.toString(), services.recordsServiceV1.getAtt(RefFieldDto(), "ref?id").asText())
        assertEquals(testRecordRef.toString(), services.recordsServiceV1.getAtt(RefFieldDto(), "ref{.id}").asText())
        assertEquals(testRecordRef.toString(), services.recordsServiceV1.getAtt(RefFieldDto(), "ref?assoc").asText())
        assertEquals(testRecordRef.toString(), services.recordsServiceV1.getAtt(RefFieldDto(), "ref{.assoc}").asText())
    }

    class RefFieldDto(
        var ref: RecordRef = testRecordRef
    )
}
