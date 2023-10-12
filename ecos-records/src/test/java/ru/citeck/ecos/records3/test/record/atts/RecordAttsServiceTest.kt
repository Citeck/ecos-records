package ru.citeck.ecos.records3.test.record.atts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.webapp.api.entity.EntityRef

class RecordAttsServiceTest {

    @Test
    fun test() {

        val recordAttsService = RecordsServiceFactory().recordsAttsService

        RequestContext.doWithCtx {

            val res0 = recordAttsService.getId(null, EntityRef.EMPTY)
            assertThat(res0).isEqualTo(EntityRef.EMPTY)

            val res1 = recordAttsService.getId("abc", EntityRef.EMPTY)
            assertThat(res1).isEqualTo(EntityRef.create("", "abc"))

            val res2 = recordAttsService.getId(
                "abc",
                EntityRef.create("source", "localId")
            )
            assertThat(res2).isEqualTo(EntityRef.create("source", "abc"))

            val res3 = recordAttsService.getId(
                "abc",
                EntityRef.create("appName", "source", "localId")
            )
            assertThat(res3).isEqualTo(EntityRef.create("appName", "source", "abc"))

            val res4 = recordAttsService.getId(
                null,
                EntityRef.create("appName", "source", "localId")
            )
            assertThat(res4).isEqualTo(EntityRef.create("appName", "source", "localId"))
        }
    }
}
