package ru.citeck.ecos.records3.test.record.dao.delete

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordMeta
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.request.delete.RecordsDelResult
import ru.citeck.ecos.records2.request.delete.RecordsDeletion
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDao
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao
import ru.citeck.ecos.test.commons.EcosWebAppApiMock
import ru.citeck.ecos.webapp.api.EcosWebAppApi

class RecordDeleteDaoTest {

    private val deletedList = mutableListOf<String>()

    @Test
    fun test() {

        val services = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi {
                return EcosWebAppApiMock("test-app")
            }
        }
        val records = services.recordsServiceV1

        records.register(object : RecordDeleteDao {
            override fun getId() = "test-delete"
            override fun delete(recordId: String): DelStatus {
                deletedList.add(recordId)
                return DelStatus.OK
            }
        })

        deleteTest("test-delete", records)

        services.recordsService.register(object : LocalRecordsDao(), MutableRecordsLocalDao<Any> {
            override fun delete(deletion: RecordsDeletion): RecordsDelResult {
                deletion.records.forEach { deletedList.add(it.id) }
                val result = RecordsDelResult()
                result.records = deletion.records.map { RecordMeta(it) }
                return result
            }
            override fun getValuesToMutate(records: MutableList<RecordRef>): MutableList<Any> {
                error("Not implemented")
            }
            override fun save(values: MutableList<Any>): RecordsMutResult {
                error("Not implemented")
            }
            override fun getId() = "legacy-dao"
        })

        deleteTest("legacy-dao", records)
    }

    private fun deleteTest(sourceId: String, records: RecordsService) {

        deletedList.clear()

        records.delete(RecordRef.valueOf("test-app/$sourceId@localId"))
        assertThat(deletedList).containsExactly("localId")

        records.delete(RecordRef.valueOf("$sourceId@localId2"))
        assertThat(deletedList).containsExactly("localId", "localId2")
    }
}
