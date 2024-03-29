package ru.citeck.ecos.records3.test.objdata

import lombok.Data
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.records2.RecordRef.Companion.valueOf
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import java.util.stream.Collectors

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ObjectDataAndDataValueTest : AbstractRecordsDao(), RecordsAttsDao {

    companion object {
        private const val JSON = "{\"a\":\"b\"}"
    }

    override fun getId(): String {
        return ""
    }

    @BeforeAll
    fun init() {
        val factory = RecordsServiceFactory()
        recordsService = factory.recordsServiceV1
        recordsService.register(this)
    }

    @Test
    fun testHasAtt() {

        val data = ObjectData.create()
        data.set("field", "value")

        assertThat(recordsService.getAtt(data, "_has.field?bool").asBoolean()).isTrue
        assertThat(recordsService.getAtt(data, "_has.field123?bool").asBoolean()).isFalse
    }

    @Test
    fun test() {

        var value = recordsService.getAtt(valueOf("test"), "data?json")
        assertEquals("b", value.get("a").asText())

        var value2 = recordsService.getAtt(valueOf("test"), "data.a?str")
        assertEquals("b", value2.asText())

        value = recordsService.getAtt(valueOf("test"), "dataValue?json")
        assertEquals("b", value.get("a").asText())

        value2 = recordsService.getAtt(valueOf("test"), "dataValue.a?str")
        assertEquals("b", value2.asText())

        value2 = recordsService.getAtt(valueOf("test"), "data.unknown?str")
        assertEquals(DataValue.NULL, value2)

        value2 = recordsService.getAtt(valueOf("test"), "dataValue.unknown?str")
        assertEquals(DataValue.NULL, value2)

        value2 = recordsService.getAtt(valueOf("test"), "unknown?str")
        assertEquals(DataValue.NULL, value2)
    }

    @Test
    fun dtoTest() {
        val dtoValue = recordsService.getAtts(valueOf("test"), TestDataMeta::class.java)
        val expected = TestDataMeta(TestData("test"))
        assertEquals(expected, dtoValue)
    }

    override fun getRecordsAtts(recordIds: List<String>): List<*> {
        return recordIds.stream()
            .map { ref: String? -> TestData(ref) }
            .collect(Collectors.toList())
    }

    @Data
    class TestData internal constructor(ref: String?) {
        val data = ObjectData.create(mapper.read(JSON, Any::class.java))
        val dataValue = DataValue.create(JSON)
    }

    data class TestDataMeta(
        var data: ObjectData? = null,
        var dataValue: DataValue? = null
    ) {
        constructor(data: TestData) : this(data.data, data.dataValue)
    }
}
