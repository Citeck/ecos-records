package ru.citeck.ecos.records3.test.op.atts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.request.RequestContext
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CtxAttributesTest {

    @Test
    fun nowTest() {

        val services = RecordsServiceFactory()

        services.recordsServiceV1.register(
            RecordsDaoBuilder.create("test")
                .addRecord(
                    "record",
                    mapOf(
                        "test" to "value",
                        "inner" to mapOf(
                            "field0" to "field0Value"
                        )
                    )
                ).build()
        )

        val nowYearValue = services.recordsServiceV1.getAtt(
            RecordRef.valueOf("test@record"), "\$now|fmt('yyyy')"
        ).asText()

        assertEquals(Calendar.getInstance().get(Calendar.YEAR).toString(), nowYearValue)

        val (nowYearValue2, aaValue) = RequestContext.doWithCtx(services, { it.withCtxAtts(mapOf("aa" to "bb")) }) {
            Pair(
                services.recordsServiceV1.getAtt(
                    RecordRef.valueOf("test@record"), "\$now|fmt('yyyy')"
                ).asText(),
                services.recordsServiceV1.getAtt(
                    RecordRef.valueOf("test@record"), "\$aa"
                ).asText()
            )
        }

        assertEquals(Calendar.getInstance().get(Calendar.YEAR).toString(), nowYearValue2)
        assertEquals("bb", aaValue)

        val nowMonthValue = services.recordsServiceV1.getAtt(
            RecordRef.valueOf("test@record"), "\$now|fmt('MM')"
        ).asText()

        val expectedMonth = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.MONTH) + 1
        assertEquals(
            if (expectedMonth < 10) {
                "0$expectedMonth"
            } else {
                "$expectedMonth"
            },
            nowMonthValue
        )

        // context attributes should be disabled for inner values
        assertTrue(
            services.recordsServiceV1.getAtt(
                RecordRef.valueOf("test@record"), "inner.\$now|fmt('MM')"
            ).isNull()
        )
    }

    @Test
    fun strTest() {
        val records = RecordsServiceFactory().recordsServiceV1
        val value = records.getAtt(RecordRef.create("meta", ""), "\$str.some-constant")
        assertThat(value.asText()).isEqualTo("some-constant")
    }

    @Test
    fun recTest() {
        val records = RecordsServiceFactory().recordsServiceV1
        val value = records.getAtt(RecordRef.create("meta", ""), "\$ref.meta@.time")
        assertThat(value.asText()).isNotBlank()
    }
}
