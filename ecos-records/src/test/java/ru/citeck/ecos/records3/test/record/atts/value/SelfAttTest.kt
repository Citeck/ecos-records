package ru.citeck.ecos.records3.test.record.atts.value

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.json.YamlUtils
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao
import java.util.*

class SelfAttTest {

    companion object {
        private const val MIME_APP_YAML = "application/x-yaml"
        private const val MIME_APP_JSON = "application/json"
    }

    @Test
    fun getAttsTest() {

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(InMemDataRecordsDao("test"))
        val testDto = TestDto(
            "value0",
            "value1",
            "value2"
        )
        val refId = records.create("test", testDto)

        val atts = records.getAtts(
            refId,
            mapOf(
                "json" to "_self?json",
                "selfField0" to "_self.field0",
                "field0" to "field0"
            )
        )

        assertThat(atts.getAtt("json")).isEqualTo(DataValue.create(testDto))
        assertThat(atts.getAtt("selfField0").asText()).isEqualTo("value0")
        assertThat(atts.getAtt("field0").asText()).isEqualTo("value0")
    }

    @Test
    fun mutationTest() {

        fun String.toBase64DataWithUrl(mimetype: String, asArray: Boolean): DataValue {
            val base64 = Base64.getEncoder().encodeToString(this.toByteArray())
            val data = DataValue.createObj()
            data.setStr("url", "data:$mimetype;base64,$base64")
            return if (asArray) {
                val arr = DataValue.createArr()
                arr.add(data)
                arr
            } else {
                data
            }
        }
        val selfDataDto = TestDto(
            "self_value0",
            "self_value1",
            "self_value2"
        )
        val selfDataBase64Json = Json.mapper.toString(selfDataDto)!!.toBase64DataWithUrl(MIME_APP_JSON, false)
        val selfDataBase64JsonArr = Json.mapper.toString(selfDataDto)!!.toBase64DataWithUrl(MIME_APP_JSON, true)
        val selfDataBase64Yaml = YamlUtils.toString(selfDataDto).toBase64DataWithUrl(MIME_APP_YAML, false)
        val selfDataBase64YamlArr = YamlUtils.toString(selfDataDto).toBase64DataWithUrl(MIME_APP_YAML, true)

        val selfDataTypes = listOf(
            selfDataDto,
            selfDataBase64Json,
            selfDataBase64JsonArr,
            selfDataBase64Yaml,
            selfDataBase64YamlArr
        )

        val records = RecordsServiceFactory().recordsServiceV1

        for (value in selfDataTypes) {

            records.register(InMemDataRecordsDao("test"))

            val recRef = records.mutate(
                "test@",
                mapOf(
                    "field0" to "value0",
                    "field1" to "value1",
                    RecordConstants.ATT_SELF to value,
                    "field2" to "value2"
                )
            )

            val dtoRes = records.getAtts(recRef, TestDto::class.java)
            assertThat(dtoRes.field0).isEqualTo("self_value0")
            assertThat(dtoRes.field1).isEqualTo("self_value1")
            assertThat(dtoRes.field2).isEqualTo("value2")
        }
    }

    data class TestDto(
        val field0: String,
        val field1: String,
        val field2: String
    )
}
