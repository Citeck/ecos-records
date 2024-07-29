package ru.citeck.ecos.records2.test.local

import com.fasterxml.jackson.databind.node.NullNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.webapp.api.entity.EntityRef
import com.fasterxml.jackson.databind.node.NullNode as JackNullNode

class NullValueTest {

    @Test
    fun test() {
        val services = RecordsServiceFactory()
        services.recordsService.register(object : RecordAttsDao {
            override fun getRecordAtts(recordId: String): Any {
                return TestDto()
            }
            override fun getId(): String {
                return "test"
            }
        })

        val records = services.recordsService
        val attsToLoad = mapOf(
            "jackNull" to "jackNull?str",
            "ecosJackNull" to "ecosJackNull?str",
            "dataValue" to "dataValue?str",
            "rawNull" to "rawNull?str"
        )
        val attsRes = records.getAtts(EntityRef.valueOf("test@abc"), attsToLoad)
        assertThat(attsRes.getAtts().size()).isEqualTo(attsToLoad.size)
        attsToLoad.forEach { (k, _) ->
            assertTrue(attsRes.getAtts().has(k))
            assertTrue(attsRes.getAtts()[k].isNull())
        }
    }

    class TestDto(
        val jackNull: JackNullNode = JackNullNode.instance,
        val ecosJackNull: NullNode = NullNode.instance,
        val dataValue: DataValue = DataValue.NULL,
        val rawNull: Any? = null
    )
}
