package ru.citeck.ecos.records3.test.record.mixin

import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.mixin.impl.mutmeta.MutMeta
import ru.citeck.ecos.records3.record.mixin.impl.mutmeta.MutMetaMixin
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.test.assertEquals

class MutMetaMixinTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()

        val dao = TestRecordsDao()
        services.recordsServiceV1.register(dao)

        val metaById = mapOf(
            "test0" to MutMeta(
                "user0",
                Instant.now().plusMillis((Random.nextFloat() * 1000).toLong()),
                "user1",
                Instant.now().plusMillis((Random.nextFloat() * 1000).toLong())
            ),
            "test1" to MutMeta(
                "user0",
                Instant.now().plusMillis((Random.nextFloat() * 1000).toLong()),
                "user1",
                Instant.now().plusMillis((Random.nextFloat() * 1000).toLong())
            )
        )

        val accessCounter = AtomicInteger()
        dao.addAttributesMixin(
            MutMetaMixin("test") {
                accessCounter.incrementAndGet()
                metaById[it]
            }
        )

        val records = services.recordsServiceV1

        RequestContext.doWithCtx(services) {
            metaById.keys.forEach { recId ->

                val creator = records.getAtt(EntityRef.valueOf("test@$recId"), RecordConstants.ATT_CREATOR).asText()
                val created = records.getAtt(EntityRef.valueOf("test@$recId"), RecordConstants.ATT_CREATED).asText()
                val modifier = records.getAtt(EntityRef.valueOf("test@$recId"), RecordConstants.ATT_MODIFIER).asText()
                val modified = records.getAtt(EntityRef.valueOf("test@$recId"), RecordConstants.ATT_MODIFIED).asText()

                val meta = MutMeta(creator, Instant.parse(created), modifier, Instant.parse(modified))

                assertEquals(metaById[recId], meta)
            }
        }
        assertEquals(2, accessCounter.get())
    }

    class TestRecordsDao : AbstractRecordsDao(), RecordAttsDao {

        override fun getId(): String = "test"

        override fun getRecordAtts(recordId: String): Any? {
            return ObjectData.create("""{"aa":"bb"}""")
        }
    }
}
