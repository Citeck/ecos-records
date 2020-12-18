package ru.citeck.ecos.records3.test.op.atts.computed

import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.op.atts.service.computed.ComputedAtt
import ru.citeck.ecos.records3.record.op.atts.service.computed.ComputedAttDef
import ru.citeck.ecos.records3.record.op.atts.service.computed.ComputedAttType
import ru.citeck.ecos.records3.record.op.atts.service.computed.StoringType
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName
import ru.citeck.ecos.records3.record.type.RecordTypeService
import kotlin.test.assertEquals

class ComputedStoringTypeTest {

    companion object {

        val type0 = RecordRef.valueOf("type0")

        const val COMP_VALUE = "computed-value"
    }

    @Test
    fun onEmptyTest() {

        val type0Atts = listOf(
            ComputedAtt(
                "computed",
                ComputedAttDef.create {
                    type = ComputedAttType.VALUE
                    storingType = StoringType.ON_EMPTY
                    config = ObjectData.create(
                        mapOf(
                            Pair("value", COMP_VALUE)
                        )
                    )
                }
            )
        )

        val services = RecordsServiceFactory()
        services.setRecordTypeService(object : RecordTypeService {
            override fun getComputedAtts(typeRef: RecordRef): List<ComputedAtt> {
                return if (typeRef == type0) {
                    type0Atts
                } else {
                    emptyList()
                }
            }
        })

        val record0 = RecordValue("type0Record", type0, "first")
        val record0Ref = RecordRef.create("test", record0.id)

        val record1 = RecordValue("type1Record", type0, "")
        val record1Ref = RecordRef.create("test", record1.id)

        val record2 = RecordValue("type2Record", type0, null)
        val record2Ref = RecordRef.create("test", record2.id)

        services.recordsServiceV1.register(
            RecordsDaoBuilder.create("test")
                .addRecord(record0.id, record0)
                .addRecord(record1.id, record1)
                .addRecord(record2.id, record2)
                .build()
        )

        assertEquals("first", services.recordsServiceV1.getAtt(record0Ref, "computed").asText())
        assertEquals(COMP_VALUE, services.recordsServiceV1.getAtt(record1Ref, "computed").asText())
        assertEquals(COMP_VALUE, services.recordsServiceV1.getAtt(record2Ref, "computed").asText())
    }

    data class RecordValue(
        val id: String,
        private val type: RecordRef,
        val computed: String?
    ) {

        @AttName("_type")
        fun getType(): RecordRef {
            return type
        }
    }
}
