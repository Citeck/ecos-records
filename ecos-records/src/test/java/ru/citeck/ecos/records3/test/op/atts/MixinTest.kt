package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.op.atts.dao.RecordAttsDao
import ru.citeck.ecos.records3.record.op.atts.service.mixin.AttMixin
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValueCtx
import ru.citeck.ecos.records3.record.op.query.dao.RecordsQueryDao
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import kotlin.test.assertEquals

class MixinTest {

    @Test
    fun singleRecordAttsSourceMixinTest() {

        val factory = RecordsServiceFactory()

        val recordsDao = object : AbstractRecordsDao(), RecordAttsDao, RecordsQueryDao {
            override fun queryRecords(query: RecordsQuery): RecsQueryRes<*>? {
                return RecsQueryRes(listOf(RecordInfo()))
            }
            override fun getRecordAtts(record: String): Any? {
                return RecordInfo()
            }
            override fun getId() = "test"
        }

        factory.recordsServiceV1.register(recordsDao)

        recordsDao.addAttributesMixin(object : AttMixin {
            override fun getAtt(path: String, value: AttValueCtx): Any? {
                if (path == "providedAtt") {
                    return "providedAtt"
                }
                return null
            }
            override fun getProvidedAtts(): Collection<String> {
                return setOf("providedAtt")
            }
        })

        val providedAtt = factory.recordsServiceV1.getAtt(RecordRef.valueOf("test@local"), "providedAtt").asText()
        assertEquals("providedAtt", providedAtt)
    }

    class RecordInfo(
        var field0: String = "field0"
    )
}
