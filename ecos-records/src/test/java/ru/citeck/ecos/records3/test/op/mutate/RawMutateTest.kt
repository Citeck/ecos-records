package ru.citeck.ecos.records3.test.op.mutate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.webapp.api.entity.EntityRef

class RawMutateTest {

    @Test
    fun test() {

        val factory = RecordsServiceFactory()
        var mutRecord = LocalRecordAtts()

        factory.recordsService.register(object : RecordMutateDao {
            override fun mutate(record: LocalRecordAtts): String {
                mutRecord = record
                return record.id
            }
            override fun getId() = "test"
        })

        val mutAtts = ObjectData.create(
            """{
                "cm:title": "title",
                "cm:title2?assoc": "title2",
                "other": "field",
                "other2?str": "field2",
                "_type": "type",
                ".att(n:\"cm:name\"){assoc}":123,
                ".att(n:\"cm:author\"){str}":456,
                ".atts(n:\"cm:author2\"){str}":true
        }
            """.trimIndent()
        )

        val mutRes = ObjectData.create(
            """{
                "cm:title": "title",
                "cm:title2": "title2",
                "other": "field",
                "other2": "field2",
                "_type": "type",
                "cm:name":123,
                "cm:author":456,
                "cm:author2":true
        }
            """.trimIndent()
        )

        val recordRef = EntityRef.valueOf("test@test")
        val resRec = factory.recordsService.mutate(recordRef, mutAtts)

        assertEquals(recordRef, resRec)
        assertEquals(recordRef.getLocalId(), mutRecord.id)
        assertEquals(mutRes, mutRecord.attributes)
    }
}
