package ru.citeck.ecos.records2.test.local

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.graphql.meta.value.MetaField
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.mixin.AttMixin
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.test.assertEquals

class NewMixinWithLegacyDaoTest {

    @Test
    fun test() {

        val factory = RecordsServiceFactory()
        val records = factory.recordsServiceV1

        val legacyDao = TestRecordsDao()
        factory.recordsService.register(legacyDao)

        val testRec0 = EntityRef.create("test", "rec0")
        var value = records.getAtt(testRec0, "field0?str").asText()
        assertEquals(testRec0.getLocalId(), value)

        value = records.getAtt(testRec0, "field1").asText()
        assertEquals("0", value)

        value = records.getAtt(testRec0, "mixinValue").asText()
        assertEquals("", value)

        val mixinValue = "MIXIN_VALUE"
        legacyDao.addAttributesMixin(object : AttMixin {
            override fun getAtt(path: String, value: AttValueCtx): Any? {
                if (path == "mixinValue") {
                    return mixinValue
                }
                return null
            }
            override fun getProvidedAtts(): Collection<String> {
                return listOf("mixinValue")
            }
        })

        value = records.getAtt(testRec0, "mixinValue").asText()
        assertEquals(mixinValue, value)
    }

    class TestRecordsDao : LocalRecordsDao(), LocalRecordsMetaDao<Any> {
        override fun getLocalRecordsMeta(records: MutableList<EntityRef>, metaField: MetaField): List<Any> {
            return records.mapIndexed { i, v -> RecordDto(v.getLocalId(), i) }
        }
        override fun getId() = "test"
    }

    data class RecordDto(
        val field0: String,
        val field1: Int
    )
}
