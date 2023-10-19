package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.test.assertEquals

class LocalIdTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1

        val dto = TestDto1(EntityRef.create("aa", "bb", "cc"), "123")
        val localId = records.getAtt(dto, "_localId").asText()

        assertEquals("cc", localId)

        val localId2 = records.getAtt(dto, "?localId").asText()

        assertEquals("cc", localId2)

        val dto2 = TestDto2("dd", "123")
        val localId3 = records.getAtt(dto2, "_localId").asText()

        assertEquals("dd", localId3)
    }

    data class TestDto1(val id: EntityRef, val field: String)
    data class TestDto2(val id: String, val field: String)
}
