package ru.citeck.ecos.records3.test.op.atts.computed

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.op.atts.service.computed.ComputedUtils
import ru.citeck.ecos.records3.record.request.RequestContext
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ComputedUtilsTest {

    @Test
    fun test() {

        RequestContext.doWithCtx(RecordsServiceFactory()) {
            assertFalse(ComputedUtils.isNewRecord())
            ComputedUtils.doWithNewRecord {
                assertTrue(ComputedUtils.isNewRecord())
            }
            assertFalse(ComputedUtils.isNewRecord())
        }
    }
}
