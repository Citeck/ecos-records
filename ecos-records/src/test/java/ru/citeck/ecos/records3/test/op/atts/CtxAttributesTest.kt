package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import java.util.*
import kotlin.test.assertEquals

class CtxAttributesTest {

    @Test
    fun nowTest() {

        val services = RecordsServiceFactory()

        services.recordsServiceV1.register(
            RecordsDaoBuilder.create("test")
                .addRecord(
                    "record",
                    mapOf(
                        Pair("test", "value")
                    )
                ).build()
        )

        val nowYearValue = services.recordsServiceV1.getAtt(
            RecordRef.valueOf("test@record"), "\$now|fmt('YYYY')"
        ).asText()

        assertEquals(Calendar.getInstance().get(Calendar.YEAR).toString(), nowYearValue)

        val nowMonthValue = services.recordsServiceV1.getAtt(
            RecordRef.valueOf("test@record"), "\$now|fmt('MM')"
        ).asText()

        assertEquals((Calendar.getInstance().get(Calendar.MONTH) + 1).toString(), nowMonthValue)
    }
}
