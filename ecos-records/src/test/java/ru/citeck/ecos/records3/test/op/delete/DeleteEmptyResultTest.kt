package ru.citeck.ecos.records3.test.op.delete

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordsDeleteDao
import ru.citeck.ecos.webapp.api.entity.EntityRef

/**
 * Tests that deleting a single record doesn't crash when the DAO returns an empty list.
 *
 * Covers a bug in AbstractRecordsService.delete(EntityRef) where `result[0]` was called
 * unconditionally after a size check that only logged a warning but didn't return early.
 * An empty result caused IndexOutOfBoundsException.
 */
class DeleteEmptyResultTest {

    @Test
    fun deleteWithEmptyResultDoesNotThrow() {

        val services = RecordsServiceFactory()

        // DAO that returns an empty list (simulates a misbehaving DAO or interceptor)
        services.recordsService.register(object : RecordsDeleteDao {
            override fun delete(records: List<String>): List<DelStatus> {
                return emptyList()
            }
            override fun getId(): String = "empty-result"
        })

        val result = services.recordsService.delete(EntityRef.create("empty-result", "rec1"))
        assertThat(result).isEqualTo(DelStatus.OK)
    }

    @Test
    fun deleteWithNormalResultStillWorks() {

        val services = RecordsServiceFactory()

        services.recordsService.register(object : RecordsDeleteDao {
            override fun delete(records: List<String>): List<DelStatus> {
                return records.map { DelStatus.OK }
            }
            override fun getId(): String = "normal"
        })

        val result = services.recordsService.delete(EntityRef.create("normal", "rec1"))
        assertThat(result).isEqualTo(DelStatus.OK)
    }
}
