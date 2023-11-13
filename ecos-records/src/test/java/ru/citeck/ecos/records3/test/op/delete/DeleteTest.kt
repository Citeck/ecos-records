package ru.citeck.ecos.records3.test.op.delete

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordMeta
import ru.citeck.ecos.records2.request.delete.RecordsDelResult
import ru.citeck.ecos.records2.request.delete.RecordsDeletion
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult
import ru.citeck.ecos.records2.request.mutation.RecordsMutation
import ru.citeck.ecos.records2.source.dao.MutableRecordsDao
import ru.citeck.ecos.records2.source.dao.RecordsDao
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao
import ru.citeck.ecos.records3.record.dao.delete.RecordsDeleteDao
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()

        val test0Deleted = ArrayList<String>()
        val test1Deleted = ArrayList<String>()

        services.recordsServiceV1.register(object : RecordsDeleteDao {
            override fun delete(records: List<String>): List<DelStatus> {
                test0Deleted.addAll(records)
                return records.map { DelStatus.OK }
            }
            override fun getId(): String = "test0"
        })

        services.recordsServiceV1.register(object : RecordDeleteDao {
            override fun delete(recordId: String): DelStatus {
                test1Deleted.add(recordId)
                return DelStatus.OK
            }
            override fun getId(): String = "test1"
        })

        val recsToDelete = arrayListOf("one", "two", "three")
        services.recordsServiceV1.delete(recsToDelete.map { EntityRef.create("test0", it) })
        assertEquals(recsToDelete, test0Deleted)
        assertTrue(test1Deleted.isEmpty())

        services.recordsServiceV1.delete(recsToDelete.map { EntityRef.create("test1", it) })
        assertEquals(recsToDelete, test1Deleted)

        val test2Deleted = ArrayList<String>()

        services.recordsService.register(object : MutableRecordsDao, RecordsDao {

            override fun mutate(mutation: RecordsMutation): RecordsMutResult {
                val res = RecordsMutResult()
                res.records = mutation.records.map { RecordMeta(it.getId()) }
                return res
            }

            override fun delete(deletion: RecordsDeletion): RecordsDelResult {
                test2Deleted.addAll(deletion.records.map { it.getLocalId() })
                val res = RecordsDelResult()
                res.records = deletion.records.map { RecordMeta(it) }
                return res
            }

            override fun getId() = "test2"
        })

        services.recordsServiceV1.delete(recsToDelete.map { EntityRef.create("test2", it) })
        assertEquals(recsToDelete, test2Deleted)
    }
}
