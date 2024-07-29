package ru.citeck.ecos.records3.test.op.delete

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao
import ru.citeck.ecos.records3.record.dao.delete.RecordsDeleteDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordsMutateWithAnyResDao
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()

        val test0Deleted = ArrayList<String>()
        val test1Deleted = ArrayList<String>()

        services.recordsService.register(object : RecordsDeleteDao {
            override fun delete(records: List<String>): List<DelStatus> {
                test0Deleted.addAll(records)
                return records.map { DelStatus.OK }
            }
            override fun getId(): String = "test0"
        })

        services.recordsService.register(object : RecordDeleteDao {
            override fun delete(recordId: String): DelStatus {
                test1Deleted.add(recordId)
                return DelStatus.OK
            }
            override fun getId(): String = "test1"
        })

        val recsToDelete = arrayListOf("one", "two", "three")
        services.recordsService.delete(recsToDelete.map { EntityRef.create("test0", it) })
        assertEquals(recsToDelete, test0Deleted)
        assertTrue(test1Deleted.isEmpty())

        services.recordsService.delete(recsToDelete.map { EntityRef.create("test1", it) })
        assertEquals(recsToDelete, test1Deleted)

        val test2Deleted = ArrayList<String>()

        services.recordsService.register(object : RecordsMutateWithAnyResDao, RecordsDeleteDao {

            override fun mutateForAnyRes(records: List<LocalRecordAtts>): List<Any> {
                return records.map { RecordAtts(it.id) }
            }

            override fun delete(recordIds: List<String>): List<DelStatus> {
                test2Deleted.addAll(recordIds)
                return recordIds.map { DelStatus.OK }
            }

            override fun getId() = "test2"
        })

        services.recordsService.delete(recsToDelete.map { EntityRef.create("test2", it) })
        assertEquals(recsToDelete, test2Deleted)
    }
}
