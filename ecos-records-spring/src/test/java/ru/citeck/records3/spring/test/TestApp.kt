package ru.citeck.records3.spring.test

import org.mockito.Mockito
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import ru.citeck.ecos.records3.RecordsProperties
import ru.citeck.ecos.webapp.api.context.EcosWebAppContext
import ru.citeck.ecos.webapp.api.promise.Promise
import ru.citeck.ecos.webapp.api.properties.EcosWebAppProperties
import ru.citeck.ecos.webapp.api.web.EcosWebClient
import java.util.UUID

@SpringBootApplication
open class TestApp {

    private val appProps = EcosWebAppProperties(
        "test-app",
        "test-app:" + UUID.randomUUID()
    )

    @Bean
    @ConditionalOnMissingBean(RecordsProperties::class)
    open fun recordsProps(): RecordsProperties {
        return RecordsProperties()
    }

    @Bean
    @ConditionalOnMissingBean
    open fun ecosWebAppContext(): EcosWebAppContext {
        val ctx = Mockito.mock(EcosWebAppContext::class.java)
        Mockito.`when`(ctx.getWebClient()).thenReturn(object : EcosWebClient {
            override fun <R : Any> execute(
                targetApp: String,
                path: String,
                request: Any,
                respType: Class<R>
            ): Promise<R> {
                error("unsupported")
            }
        })
        Mockito.`when`(ctx.getProperties()).thenReturn(appProps)
        return ctx
    }
}
