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
import ru.citeck.ecos.test.commons.EcosWebAppApiMock
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import ru.citeck.ecos.webapp.api.entity.EntityRef

class ComputedAssocTest {

    @Test
    fun test() {

        val testType = EntityRef.create("emodel", "type", "test")

        val services = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi {
                return EcosWebAppApiMock()
            }
        }
        val typeInfo = object : RecordTypeInfo {
            override fun getSourceId() = ""
            override fun getParentId(): String = ""
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
                    },
                    RecordComputedAtt.create {
                        withId("authority-null")
                        withConfig(
                            ObjectData.create()
                                .set("fn", "return null;")
                        )
                        withType(RecordComputedAttType.SCRIPT)
                        withResultType(RecordComputedAttResType.AUTHORITY)
                    },
                    RecordComputedAtt.create {
                        withId("authority-admin")
                        withConfig(
                            ObjectData.create()
                                .set("fn", "return 'admin';")
                        )
                        withType(RecordComputedAttType.SCRIPT)
                        withResultType(RecordComputedAttResType.AUTHORITY)
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

        val records = services.recordsService

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
        assertThat(result["?id"].asText()).isEqualTo("test-app/test@counterparty")

        val result1 = records.getAtt(ref1, "authority-null?str")
        assertThat(result1.asText()).isEqualTo("")
        val result2 = records.getAtt(ref1, "authority-admin?str")
        assertThat(result2.asText()).isEqualTo("emodel/person@admin")
    }
}
