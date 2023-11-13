package ru.citeck.ecos.records3.test.op.atts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.test.assertEquals

class AttSchemaReaderTest {

    @Test
    fun test() {

        val factory = RecordsServiceFactory()
        val reader = factory.attSchemaReader

        val atts = reader.read("aa{bb:cc,dd:ee,}")
        assertEquals(2, atts.inner.size)
        assertEquals("bb", atts.inner[0].alias)
        assertEquals("cc", atts.inner[0].name)
        assertEquals("dd", atts.inner[1].alias)
        assertEquals("ee", atts.inner[1].name)
    }

    @Test
    fun partiallyIncorrectSchemaTest() {

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1
        records.register(
            RecordsDaoBuilder.create("test")
                .addRecord("test", mapOf("att" to "value"))
                .build()
        )

        val check = { atts: RecordAtts ->
            assertThat(atts.getAtt("correct").asText()).isEqualTo("value")
            assertThat(atts.getAtt("incorrect").isNull()).isTrue
        }

        val ref = EntityRef.valueOf("test@test")
        val attsToReq = mapOf(
            "correct" to "att",
            "incorrect" to "att{"
        )
        check(records.getAtts(ref, attsToReq))
        check(
            records.query(
                RecordsQuery.create {
                    withSourceId("test")
                    withQuery(Predicates.eq("att", "value"))
                },
                attsToReq
            ).getRecords()[0]
        )

        val schemaAtts = services.attSchemaReader.read(attsToReq)
        assertThat(schemaAtts).hasSize(2)
        assertThat(schemaAtts[0].name).isEqualTo("att")
        assertThat(schemaAtts[1].name).isEqualTo("_null")
    }
}
