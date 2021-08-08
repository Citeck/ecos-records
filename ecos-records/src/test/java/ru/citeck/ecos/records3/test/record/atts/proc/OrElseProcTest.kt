package ru.citeck.ecos.records3.test.record.atts.proc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory

class OrElseProcTest {

    @Test
    fun orElseTest() {

        val records = RecordsServiceFactory().recordsServiceV1
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

        val ref = RecordRef.create("test", "test")

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
