package ru.citeck.ecos.records3.test.op.mutate

import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.op.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.op.mutate.dao.RecordMutateDao
import kotlin.test.assertEquals

class RawMutateTest {

    @Test
    fun test() {

        val factory = RecordsServiceFactory()
        var mutRecord = LocalRecordAtts()

        factory.recordsServiceV1.register(object : RecordMutateDao {
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

        val recordRef = RecordRef.valueOf("test@test")
        val resRec = factory.recordsServiceV1.mutate(recordRef, mutAtts)

        assertEquals(recordRef, resRec)
        assertEquals(recordRef.id, mutRecord.id)
        assertEquals(mutRes, mutRecord.attributes)
    }
}
