package ru.citeck.ecos.records3.test.record.ref

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.entity.SimpleEntityRef
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao
import ru.citeck.ecos.webapp.api.entity.EntityRef

class RecordEntityRefFactoryTest {

    @Test
    fun test() {
        val ref = EntityRef.valueOf("app/src@id")
        assertThat(ref is RecordRef).isTrue
        assertThat(ref).isEqualTo(RecordRef.valueOf("app/src@id"))

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(InMemDataRecordsDao("test"))

        val recordRef = records.create("test", mapOf("att" to "value"))
        assertThat(records.getAtt(recordRef, "att").asText()).isEqualTo("value")
        val otherRef = SimpleEntityRef("", "test", recordRef.id)
        assertThat(records.getAtt(otherRef, "att").asText()).isEqualTo("value")
    }
}
