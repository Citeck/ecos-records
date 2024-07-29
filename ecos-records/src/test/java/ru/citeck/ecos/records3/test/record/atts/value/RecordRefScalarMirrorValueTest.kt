package ru.citeck.ecos.records3.test.record.atts.value

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.test.assertEquals

class RecordRefScalarMirrorValueTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()
        val records = services.recordsService

        records.register(
            RecordsDaoBuilder.create("test")
                .addRecord("test-rec", TestWithDisp())
                .build()
        )

        checkAtt("ref?disp", "abc", records)
        checkAtt("ref._disp", "abc", records)
    }

    fun checkAtt(att: String, expected: String, records: RecordsService) {
        val text = records.getAtt(TestDto(EntityRef.valueOf("test@test-rec")), att).asText()
        assertEquals(expected, text)
    }

    class TestDto(val ref: EntityRef)

    class TestWithDisp {

        fun getDisplayName(): String {
            return "abc"
        }
    }
}
