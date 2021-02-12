package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue
import ru.citeck.ecos.records3.record.request.RequestContext
import java.util.*
import kotlin.test.assertEquals

class DisplayNameTest {

    companion object {
        private const val RU_DISP = "Русский"
        private const val EN_DISP = "English"
        private val ML_DISP = MLText.EMPTY
            .withValue(Locale.ENGLISH, EN_DISP)
            .withValue(Locale("ru"), RU_DISP)
    }

    @Test
    fun test() {

        val factory = RecordsServiceFactory()
        val records = factory.recordsServiceV1

        assertEquals(RU_DISP, records.getAtt(AttValueStrRuDispClass(), "?disp").asText())
        assertEquals(EN_DISP, records.getAtt(AttValueMlDispClass(), "?disp").asText())
        RequestContext.doWithCtx(factory, { it.withLocale(Locale("ru")) }) {
            assertEquals(RU_DISP, records.getAtt(AttValueMlDispClass(), "?disp").asText())
        }

        assertEquals(RU_DISP, records.getAtt(DtoStrDispClass(), "?disp").asText())
        assertEquals(EN_DISP, records.getAtt(DtoMLDispClass(), "?disp").asText())
        RequestContext.doWithCtx(factory, { it.withLocale(Locale("ru")) }) {
            assertEquals(RU_DISP, records.getAtt(DtoMLDispClass(), "?disp").asText())
        }

        RequestContext.doWithCtx(factory, { it.withLocale(Locale.FRANCE) }) {
            assertEquals(EN_DISP, records.getAtt(DtoMLDispClass(), "?disp").asText())
        }

        records.register(
            RecordsDaoBuilder.create("test")
                .addRecord("test", DtoMLDispClass())
                .build()
        )

        assertEquals(EN_DISP, records.getAtt(DtoWithLinkToDtoMLDispClass(), "link?disp").asText())
        RequestContext.doWithCtx(factory, { it.withLocale(Locale("ru")) }) {
            assertEquals(RU_DISP, records.getAtt(DtoWithLinkToDtoMLDispClass(), "link?disp").asText())
        }
    }

    class AttValueStrRuDispClass : AttValue {
        override fun getDisplayName(): Any? {
            return RU_DISP
        }
    }

    class AttValueMlDispClass : AttValue {
        override fun getDisplayName(): Any? {
            return ML_DISP
        }
    }

    class DtoStrDispClass(
        @AttName("?disp")
        val name: String = RU_DISP
    )

    class DtoMLDispClass(
        @AttName("?disp")
        val name: MLText = ML_DISP
    )

    class DtoWithLinkToDtoMLDispClass(
        val link: RecordRef = RecordRef.valueOf("test@test")
    )
}
