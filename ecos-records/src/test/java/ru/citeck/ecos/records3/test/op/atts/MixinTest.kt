package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.mixin.AttMixin
import kotlin.test.assertEquals

class MixinTest {

    @Test
    fun singleRecordAttsSourceMixinTest() {

        val factory = RecordsServiceFactory()

        val recordsDao = object : AbstractRecordsDao(), RecordAttsDao, RecordsQueryDao {
            override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>? {
                return RecsQueryRes(listOf(RecordInfo()))
            }
            override fun getRecordAtts(recordId: String): Any? {
                return RecordInfo(
                    inner = RecordInfo()
                )
            }
            override fun getId() = "test"
        }

        factory.recordsServiceV1.register(recordsDao)

        val field0Title = "field0_Title"
        recordsDao.addAttributesMixin(object : AttMixin {
            override fun getAtt(path: String, value: AttValueCtx): Any? {
                if (path == "providedAtt" || path == "*innerProvided") {
                    return "providedAtt"
                }
                if (path == "_edge.field0.title") {
                    return field0Title
                }
                return null
            }
            override fun getProvidedAtts(): Collection<String> {
                return setOf(
                    "providedAtt",
                    "*innerProvided",
                    "_edge.field0.title"
                )
            }
        })

        val ref = RecordRef.valueOf("test@local")

        val providedAtt = factory.recordsServiceV1.getAtt(ref, "providedAtt").asText()
        assertEquals("providedAtt", providedAtt)

        val innerProvidedAtt = factory.recordsServiceV1.getAtt(ref, "inner.providedAtt").asText()
        assertEquals("", innerProvidedAtt)

        val providedInnerAtt = factory.recordsServiceV1.getAtt(ref, "innerProvided").asText()
        assertEquals("providedAtt", providedInnerAtt)

        val innerProvidedInnerAtt = factory.recordsServiceV1.getAtt(ref, "inner.innerProvided").asText()
        assertEquals("providedAtt", innerProvidedInnerAtt)

        val field0TitleFromRec = factory.recordsServiceV1.getAtt(ref, "_edge.field0.title").asText()
        assertEquals(field0Title, field0TitleFromRec)

        val field0TitleFromRec2 = factory.recordsServiceV1.getAtt(ref, ".edge(n:\"field0\"){title}").asText()
        assertEquals(field0Title, field0TitleFromRec2)
    }

    class RecordInfo(
        var field0: String = "field0",
        val inner: RecordInfo? = null
    )
}
