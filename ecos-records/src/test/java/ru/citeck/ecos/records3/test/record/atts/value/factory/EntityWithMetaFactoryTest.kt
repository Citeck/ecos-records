package ru.citeck.ecos.records3.test.record.atts.value.factory

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.entity.EntityMeta
import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.RecordsServiceFactory
import java.time.Instant

class EntityWithMetaFactoryTest {

    @Test
    fun test() {

        val entity = Entity("some-entity")
        val meta = EntityMeta.create()
            .withCreated(Instant.parse("2022-02-02T11:22:33Z"))
            .withCreator("user-0")
            .withModified(Instant.parse("2022-02-02T22:33:44Z"))
            .withModifier("user-1")
            .build()

        val rec = EntityWithMeta(entity, meta)
        val records = RecordsServiceFactory().recordsService
        assertThat(records.getAtt(rec, RecordConstants.ATT_CREATED).getAsInstant()).isEqualTo(meta.created)
        assertThat(records.getAtt(rec, RecordConstants.ATT_CREATOR + "?localId").asText()).isEqualTo("user-0")
        assertThat(records.getAtt(rec, RecordConstants.ATT_MODIFIED).getAsInstant()).isEqualTo(meta.modified)
        assertThat(records.getAtt(rec, RecordConstants.ATT_MODIFIER + "?localId").asText()).isEqualTo("user-1")
    }

    class Entity(
        val name: String
    )
}
