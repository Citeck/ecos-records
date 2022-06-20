package ru.citeck.ecos.records2.test.local

// import com.fasterxml.jackson.databind.JsonNode
import ecos.com.fasterxml.jackson210.databind.node.NullNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.graphql.meta.value.MetaField
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao
import ru.citeck.ecos.records3.RecordsServiceFactory
import com.fasterxml.jackson.databind.node.NullNode as JackNullNode

class NullValueTest {

    @Test
    fun test() {
        val services = RecordsServiceFactory()
        services.recordsService.register(object : LocalRecordsDao(), LocalRecordsMetaDao<TestDto> {
            override fun getLocalRecordsMeta(
                records: MutableList<RecordRef>,
                metaField: MetaField
            ): List<TestDto> {
                return records.map { TestDto() }
            }
            override fun getId(): String {
                return "test"
            }
        })

        val records = services.recordsServiceV1
        val attsToLoad = mapOf(
            "jackNull" to "jackNull?str",
            "ecosJackNull" to "ecosJackNull?str",
            "dataValue" to "dataValue?str",
            "rawNull" to "rawNull?str"
        )
        val attsRes = records.getAtts(RecordRef.valueOf("test@abc"), attsToLoad)
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
