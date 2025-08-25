package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.mixin.AttMixin
import ru.citeck.ecos.webapp.api.entity.EntityRef

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

        factory.recordsService.register(recordsDao)

        val field0Title = "field0_Title"
        val displayName = "Display Name"

        recordsDao.addAttributesMixin(object : AttMixin {
            override fun getAtt(path: String, value: AttValueCtx): Any? {
                if (path == "providedAtt" || path == "*innerProvided") {
                    return "providedAtt"
                }
                if (path == "_edge.field0.title") {
                    return field0Title
                }
                if (path == "_disp") {
                    return displayName
                }
                return null
            }
            override fun getProvidedAtts(): Collection<String> {
                return setOf(
                    "_disp",
                    "providedAtt",
                    "*innerProvided",
                    "_edge.field0.title"
                )
            }
        })

        val ref = EntityRef.valueOf("test@local")

        val providedAtt = factory.recordsService.getAtt(ref, "providedAtt").asText()
        assertEquals("providedAtt", providedAtt)

        val innerProvidedAtt = factory.recordsService.getAtt(ref, "inner.providedAtt").asText()
        assertEquals("", innerProvidedAtt)

        val providedInnerAtt = factory.recordsService.getAtt(ref, "innerProvided").asText()
        assertEquals("providedAtt", providedInnerAtt)

        val innerProvidedInnerAtt = factory.recordsService.getAtt(ref, "inner.innerProvided").asText()
        assertEquals("providedAtt", innerProvidedInnerAtt)

        val field0TitleFromRec = factory.recordsService.getAtt(ref, "_edge.field0.title").asText()
        assertEquals(field0Title, field0TitleFromRec)

        val field0TitleFromRec2 = factory.recordsService.getAtt(ref, ".edge(n:\"field0\"){title}").asText()
        assertEquals(field0Title, field0TitleFromRec2)

        val dispNameScalar = factory.recordsService.getAtt(ref, "?disp").asText()
        assertEquals(displayName, dispNameScalar)

        val dispNameAtt = factory.recordsService.getAtt(ref, "_disp").asText()
        assertEquals(displayName, dispNameAtt)
    }

    class RecordInfo(
        var field0: String = "field0",
        val inner: RecordInfo? = null
    )
}
