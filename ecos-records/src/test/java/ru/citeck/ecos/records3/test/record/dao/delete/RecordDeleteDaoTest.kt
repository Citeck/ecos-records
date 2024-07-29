package ru.citeck.ecos.records3.test.record.dao.delete

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao
import ru.citeck.ecos.test.commons.EcosWebAppApiMock
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import ru.citeck.ecos.webapp.api.entity.EntityRef

class RecordDeleteDaoTest {

    private val deletedList = mutableListOf<String>()

    @Test
    fun test() {

        val services = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi {
                return EcosWebAppApiMock("test-app")
            }
        }
        val records = services.recordsService

        records.register(object : RecordDeleteDao {
            override fun getId() = "test-delete"
            override fun delete(recordId: String): DelStatus {
                deletedList.add(recordId)
                return DelStatus.OK
            }
        })

        deleteTest("test-delete", records)

        services.recordsService.register(object : RecordDeleteDao {
            override fun delete(recordId: String): DelStatus {
                deletedList.add(recordId)
                return DelStatus.OK
            }
            override fun getId() = "legacy-dao"
        })

        deleteTest("legacy-dao", records)
    }

    private fun deleteTest(sourceId: String, records: RecordsService) {

        deletedList.clear()

        records.delete(EntityRef.valueOf("test-app/$sourceId@localId"))
        assertThat(deletedList).containsExactly("localId")

        records.delete(EntityRef.valueOf("$sourceId@localId2"))
        assertThat(deletedList).containsExactly("localId", "localId2")
    }
}
