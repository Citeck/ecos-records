package ru.citeck.ecos.records3.test.record.ref

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.entity.SimpleEntityRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao
import ru.citeck.ecos.webapp.api.entity.EntityRef

class RecordEntityRefFactoryTest {

    @Test
    fun test() {
        val ref = EntityRef.valueOf("app/src@id")
        assertThat(ref is EntityRef).isTrue
        assertThat(ref).isEqualTo(EntityRef.valueOf("app/src@id"))

        val records = RecordsServiceFactory().recordsService
        records.register(InMemDataRecordsDao("test"))

        val recordRef = records.create("test", mapOf("att" to "value"))
        assertThat(records.getAtt(recordRef, "att").asText()).isEqualTo("value")
        val otherRef = SimpleEntityRef("", "test", recordRef.getLocalId())
        assertThat(records.getAtt(otherRef, "att").asText()).isEqualTo("value")

        records.register(object : RecordAttsDao {
            override fun getId() = "test2"
            override fun getRecordAtts(recordId: String): Any {
                return RecordDto(recordId)
            }
        })

        assertThat(
            records.getAtt(
                EntityRef.create("test2", recordRef.getLocalId()),
                "ref.att"
            ).asText()
        ).isEqualTo("value")
    }

    class RecordDto(id: String) {
        val ref: EntityRef = SimpleEntityRef("", "test", id)
    }
}
