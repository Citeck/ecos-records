package ru.citeck.ecos.records3.test

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.request.RequestContext

class DefaultRequestContextTest {

    @Test
    fun test() {

        RequestContext.setDefaultServices(null)
        RequestContext.setLastCreatedServices(null)

        assertThrows<Exception> { RequestContext.doWithCtx {} }

        val services0 = RecordsServiceFactory()

        RequestContext.doWithCtx { ctx ->
            assertThat(ctx.getServices()).isSameAs(services0)
        }

        val services1 = RecordsServiceFactory()

        RequestContext.doWithCtx { ctx ->
            assertThat(ctx.getServices()).isSameAs(services1)
        }

        RequestContext.setDefaultServices(services0)

        RequestContext.doWithCtx { ctx ->
            assertThat(ctx.getServices()).isSameAs(services0)
        }

        val services2 = RecordsServiceFactory()

        RequestContext.doWithCtx { ctx ->
            assertThat(ctx.getServices()).isSameAs(services0)
        }
    }
}
