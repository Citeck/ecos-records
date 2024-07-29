package ru.citeck.ecos.records3.test.record.request.ctxatts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.request.ctxatts.CtxAttsProvider
import java.time.LocalDate

class CtxAttsServiceTest {

    @Test
    fun test() {

        val key = "ctx-key"

        val services = RecordsServiceFactory()
        services.ctxAttsService.register(object : CtxAttsProvider {
            override fun fillContextAtts(attributes: MutableMap<String, Any?>) {
                attributes[key] = "123"
                attributes["now"] = 0 // check that providers with default order doesn't override std atts
            }
            override fun getOrder() = 1f
        })

        val res = services.recordsService.getAtt("", "\$$key").asText()
        assertThat(res).isEqualTo("123")

        val res2 = services.recordsService.getAtt("", "\$now|fmt('yyyy')").asInt()
        assertThat(res2).isEqualTo(LocalDate.now().year)

        fun createOverrideProv(order: Float): CtxAttsProvider {
            return object : CtxAttsProvider {
                override fun fillContextAtts(attributes: MutableMap<String, Any?>) {
                    attributes[key] = "456"
                }
                override fun getOrder() = order
            }
        }

        // this provider (0) will be less important than main (1)
        services.ctxAttsService.register(createOverrideProv(0f))

        val res3 = services.recordsService.getAtt("", "\$$key").asText()
        assertThat(res3).isEqualTo("123")

        // this provider (5) will be more important than main (1)
        services.ctxAttsService.register(createOverrideProv(5f))

        val res4 = services.recordsService.getAtt("", "\$$key").asText()
        assertThat(res4).isEqualTo("456")
    }
}
