package ru.citeck.ecos.records3.test.bean

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.op.atts.dao.RecordAttsDao
import ru.citeck.ecos.records3.record.op.query.dao.RecordsQueryDao
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import kotlin.test.assertEquals

class BeanValueFactoryTest2 {

    @Test
    fun test() {

        val services = RecordsServiceFactory()

        services.recordsServiceV1.register(object : RecordsQueryDao, RecordAttsDao {
            override fun queryRecords(query: RecordsQuery): RecsQueryRes<*>? {
                return RecsQueryRes.of(TestClass(RecordRef.valueOf("test@test")))
            }
            override fun getRecordAtts(record: String): Any? {
                return TestClass(RecordRef.valueOf("test@test"))
            }
            override fun getId() = "test"
        })

        assertEquals("test@test", services.recordsServiceV1.getAtt(RecordRef.valueOf("test@test"), "?id").asText())
        val queryRecord = services.recordsServiceV1.query(
            RecordsQuery.create {
                sourceId = "test"
            },
            mapOf(Pair("id", "?id"))
        ).getRecords()[0]

        assertEquals("test@test", queryRecord.getAtt("id").asText())
        assertEquals("test@test", queryRecord.getId().toString())
    }

    class TestClass(val id: RecordRef) {

        fun getAtt(): String {
            return id.id
        }
    }
}
