package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.test.assertEquals

class RecordRefAttTest {

    companion object {
        val testRecordRef = EntityRef.create("test-app", "sourceId", "test")
        val testRecordRef2 = EntityRef.create("test-app", "", "test")
    }

    @Test
    fun test() {

        val services = RecordsServiceFactory()

        services.recordsServiceV1.register(object : RecordAttsDao {
            override fun getRecordAtts(record: String): Any? {
                return ObjectData.create()
            }
            override fun getId() = testRecordRef.getSourceId()
        })

        assertEquals(testRecordRef.toString(), services.recordsServiceV1.getAtt(RefFieldDto(), "ref?id").asText())
        assertEquals(testRecordRef.toString(), services.recordsServiceV1.getAtt(RefFieldDto(), "ref{.id}").asText())
        assertEquals(testRecordRef.toString(), services.recordsServiceV1.getAtt(RefFieldDto(), "ref?assoc").asText())
        assertEquals(testRecordRef.toString(), services.recordsServiceV1.getAtt(RefFieldDto(), "ref{.assoc}").asText())
    }

    @Test
    fun testWithEmptySourceId() {

        val services = RecordsServiceFactory()

        services.recordsServiceV1.register(
            RecordsDaoBuilder.create("test")
                .addRecord("record", RefFieldDto2())
                .build()
        )

        val recordRef = services.recordsServiceV1.queryOne(
            RecordsQuery.create {
                withSourceId("test")
                withQuery(VoidPredicate.INSTANCE)
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
            },
            "ref?id"
        )

        assertEquals(testRecordRef2.toString(), recordRef.asText())
    }

    class RefFieldDto(
        var ref: EntityRef = testRecordRef
    )

    class RefFieldDto2(
        var ref: EntityRef = testRecordRef2
    )
}
