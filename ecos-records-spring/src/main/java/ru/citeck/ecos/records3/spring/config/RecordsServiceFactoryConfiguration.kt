package ru.citeck.ecos.records3.spring.config

import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.records2.RecordsService
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.querylang.QueryLangService
import ru.citeck.ecos.records2.request.rest.RestHandler
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDaoAttsProvider
import ru.citeck.ecos.records3.RecordsProperties
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.ctxatts.CtxAttsProvider
import ru.citeck.ecos.records3.rest.RestHandlerAdapter
import ru.citeck.ecos.webapp.api.context.EcosWebAppContext
import java.util.*
import javax.annotation.PostConstruct

@Slf4j
@Configuration
open class RecordsServiceFactoryConfiguration : RecordsServiceFactory() {

    private lateinit var props: RecordsProperties

    private var customCtxAttsProviders: List<CtxAttsProvider> = emptyList()
    private lateinit var ecosWebAppContext: EcosWebAppContext

    @PostConstruct
    fun init() {
        RequestContext.setDefaultServices(this)
    }

    override fun createCtxAttsProviders(): List<CtxAttsProvider> {
        val providers = ArrayList(super.createCtxAttsProviders())
        providers.addAll(customCtxAttsProviders)
        return providers
    }

    override fun getEcosWebAppContext(): EcosWebAppContext? {
        return ecosWebAppContext
    }

    @Bean
    override fun createRecordEvaluatorService(): RecordEvaluatorService {
        return super.createRecordEvaluatorService()
    }

    @Bean
    override fun createRestHandler(): RestHandler {
        return super.createRestHandler()
    }

    @Bean
    override fun createRecordsService(): RecordsService {
        return super.createRecordsService()
    }

    @Bean
    override fun createRecordsServiceV1(): ru.citeck.ecos.records3.RecordsService {
        return super.createRecordsServiceV1()
    }

    @Bean
    override fun createQueryLangService(): QueryLangService {
        return super.createQueryLangService()
    }

    @Bean
    override fun createPredicateService(): PredicateService {
        return super.createPredicateService()
    }

    @Bean
    override fun createMetaRecordsDaoAttsProvider(): MetaRecordsDaoAttsProvider {
        return super.createMetaRecordsDaoAttsProvider()
    }

    @Bean
    override fun createRestHandlerAdapter(): RestHandlerAdapter {
        return super.createRestHandlerAdapter()
    }

    override fun createProperties(): RecordsProperties {
        return this.props
    }

    @Autowired
    fun setProperties(props: RecordsProperties) {
        this.props = props
    }

    @Autowired(required = false)
    fun setCtxAttsProviders(providers: List<CtxAttsProvider>) {
        customCtxAttsProviders = providers
    }

    @Autowired
    fun setEcosWebAppContext(ecosWebAppContext: EcosWebAppContext) {
        this.ecosWebAppContext = ecosWebAppContext
    }
}
