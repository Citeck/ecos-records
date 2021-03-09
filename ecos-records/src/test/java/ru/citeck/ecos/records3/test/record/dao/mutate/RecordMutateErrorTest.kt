package ru.citeck.ecos.records3.test.record.dao.mutate

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import java.lang.IllegalArgumentException
import java.lang.RuntimeException
import kotlin.test.assertEquals

class RecordMutateErrorTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1

        records.register(object : RecordMutateDao {

            override fun mutate(record: LocalRecordAtts): String {
                val message = record.attributes.get("message").asText()
                if (record.id == "Runtime") {
                    throw RuntimeException(message)
                } else if (record.id == "IllegalArg") {
                    throw IllegalArgumentException(message)
                }
                return message
            }

            override fun getId() = "mut-error-test"
        })

        val exceptionMessage = "error-message"
        val attributes = ObjectData.create()
        attributes.set("message", exceptionMessage)

        val runtimeEx = assertThrows<RuntimeException> {
            records.mutate(RecordRef.valueOf("mut-error-test@Runtime"), attributes)
        }
        assertEquals(exceptionMessage, runtimeEx.message)

        val illegalArgEx = assertThrows<RuntimeException> {
            records.mutate(RecordRef.valueOf("mut-error-test@IllegalArg"), attributes)
        }
        assertEquals(exceptionMessage, illegalArgEx.message)

        val withoutEx = records.mutate(RecordRef.valueOf("mut-error-test@"), attributes)
        assertEquals(RecordRef.valueOf("mut-error-test@$exceptionMessage"), withoutEx)
    }
}
