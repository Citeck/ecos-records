package ru.citeck.ecos.records3.test.record.atts.proc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.proc.AttProcDef
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.webapp.api.entity.EntityRef

class OrElseProcTest {

    @Test
    fun preserveDisplayNameStringWithFallback() {
        val records = RecordsServiceFactory().recordsService
        listOf("{\"ru\":\"Название процесса\"}", "[1,2]", "true", "123", "null").forEach { name ->
            val record = object : AttValue {
                override fun getDisplayName(): Any = MLText(name)
            }
            val result = records.getAtt(record, "?disp!_type?disp!?localId")
            assertThat(result).describedAs(name).isEqualTo(DataValue.createStr(name))
        }
    }

    @Test
    fun preserveStringInProcessedFallbackAttribute() {
        val service = RecordsServiceFactory().attProcService
        val name = "{\"ru\":\"Название процесса\"}"
        val result = service.applyProcessors(
            mapOf("displayName" to null, "__proc_att_fallback" to name),
            linkedMapOf(
                "__proc_att_fallback" to listOf(AttProcDef("or", listOf(DataValue.createStr("unused")))),
                "displayName" to listOf(AttProcDef("or", listOf(DataValue.createStr("a:fallback"))))
            )
        )
        assertThat(result["displayName"]).isEqualTo(DataValue.createStr(name))
    }

    @Test
    fun preserveBinaryValueWithFallback() {
        val bytes = "{\"ru\":\"Название процесса\"}".toByteArray()
        val result = RecordsServiceFactory().attProcService.applyProcessors(
            mapOf("value" to bytes),
            mapOf("value" to listOf(AttProcDef("or", listOf(DataValue.createStr("unused")))))
        )
        assertThat(result["value"]).isEqualTo(DataValue.createAsIs(bytes))
    }

    @Test
    fun preserveRawValuesWithFallback() {
        val records = RecordsServiceFactory().recordsService
        listOf(
            DataValue.createObj().set("ru", "Название процесса"),
            DataValue.createArr().add(1),
            DataValue.TRUE,
            DataValue.create(123)
        ).forEach { value ->
            val attribute = if (value.isArray()) "value[]?raw!'unused'" else "value?raw!'unused'"
            assertThat(records.getAtt(ObjectData.create().set("value", value), attribute))
                .isEqualTo(value)
        }
    }

    @Test
    fun autoOrElseTest() {
        val records = RecordsServiceFactory().recordsService
        assertThat(records.getAtt(null, "abc?bool!")).isEqualTo(DataValue.FALSE)
        assertThat(records.getAtt(null, "abc?num!")).isEqualTo(DataValue.create(0.0))
        assertThat(records.getAtt(null, "abc?json!")).isEqualTo(DataValue.createObj())
        listOf(
            ScalarType.STR,
            ScalarType.RAW,
            ScalarType.BIN,
            ScalarType.ID,
            ScalarType.DISP,
            ScalarType.ASSOC,
            ScalarType.LOCAL_ID,
            ScalarType.APP_NAME,
            ScalarType.LOCAL_SRC_ID
        ).forEach {
            assertThat(records.getAtt(null, "abc${it.schema}!"))
                .describedAs(it.schema)
                .isEqualTo(DataValue.create(""))
        }
        assertThat(records.getAtt(null, "abc[]?bool!")).isEqualTo(DataValue.createArr())
        assertThat(records.getAtt(null, "abc{def,hij}!")).isEqualTo(DataValue.createObj())
    }

    @Test
    fun orElseTest() {

        val records = RecordsServiceFactory().recordsService
        records.register(
            RecordsDaoBuilder.create("test")
                .addRecord(
                    "test",
                    ObjectData.create(
                        """
                            {
                                "attributes": [
                                    {
                                        "id": "some-id",
                                        "name": ""
                                    }
                                ]
                            }
                        """.trimMargin()
                    )
                )
                .build()
        )

        val ref = EntityRef.create("test", "test")

        val res: DataValue = records.getAtt(ref, "attributes[]{value:id,label:name}")
        assertThat(res).isEqualTo(
            DataValue.create(
                """
                [
                        {
                            "value": "some-id",
                            "label": ""
                        }
                ]
                """.trimMargin()
            )
        )

        val res2: DataValue = records.getAtt(
            ref,
            "attributes[]{" +
                "value:id," +
                "label:name!id," +
                "label2:name|or('a:id')," +
                "label3:name!unknown_att!'constant'" +
                "}"
        )
        assertThat(res2).isEqualTo(
            DataValue.create(
                """
                [
                    {
                        "value": "some-id",
                        "label": "some-id",
                        "label2": "some-id",
                        "label3": "constant"
                    }
                ]
                """.trimMargin()
            )
        )
    }
}
