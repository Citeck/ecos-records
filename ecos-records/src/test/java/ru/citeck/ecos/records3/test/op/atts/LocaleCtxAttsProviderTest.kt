package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.request.RequestContext
import java.util.*
import kotlin.test.assertEquals

class LocaleCtxAttsProviderTest {

    @Test
    fun test() {

        var locale = Locale.ENGLISH

        val services = object : RecordsServiceFactory() {
            override fun createLocaleSupplier(): () -> Locale {
                return { locale }
            }
        }
        val records = services.recordsServiceV1
        records.register(object : RecordAttsDao {
            override fun getRecordAtts(record: String): Any? = emptyMap<String, Any>()
            override fun getId(): String = "test"
        })

        RequestContext.doWithCtx(services) {
            assertEquals(locale, it.getCtxLocale())
        }

        locale = Locale.CANADA

        RequestContext.doWithCtx(services) {
            assertEquals(locale, it.getCtxLocale())
        }
    }
}
