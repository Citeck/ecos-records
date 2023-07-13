package ru.citeck.ecos.records3.test.local

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAtt
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAttResType
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAttType
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao
import ru.citeck.ecos.records3.record.type.RecordTypeComponent
import ru.citeck.ecos.records3.record.type.RecordTypeInfo
import ru.citeck.ecos.webapp.api.entity.EntityRef

class ComputedAssocTest {

    @Test
    fun test() {

        val testType = EntityRef.create("emodel", "type", "test")

        val services = RecordsServiceFactory()
        val typeInfo = object : RecordTypeInfo {
            override fun getSourceId() = ""
            override fun getComputedAtts(): List<RecordComputedAtt> {
                return listOf(
                    RecordComputedAtt.create {
                        withId("counterparty")
                        withConfig(
                            ObjectData.create()
                                .set("attribute", "counterpartyAssoc?id")
                        )
                        withType(RecordComputedAttType.ATTRIBUTE)
                        withResultType(RecordComputedAttResType.REF)
                    }
                )
            }
        }
        services.setRecordTypeComponent(object : RecordTypeComponent {
            override fun getRecordType(typeRef: EntityRef): RecordTypeInfo? {
                if (typeRef == testType) {
                    return typeInfo
                }
                return null
            }
        })

        val records = services.recordsServiceV1

        records.register(InMemDataRecordsDao("test"))
        records.create(
            "test",
            mapOf(
                "id" to "counterparty",
                "name" to "Counterparty Name"
            )
        )
        val ref1 = records.create(
            "test",
            mapOf(
                "id" to "rec0",
                "counterpartyAssoc" to "test@counterparty",
                "_type" to testType
            )
        )

        val result = records.getAtt(ref1, "counterparty{?disp,?id}")
        assertThat(result["?disp"].asText()).isEqualTo("Counterparty Name")
        assertThat(result["?id"].asText()).isEqualTo("test@counterparty")
    }
}
