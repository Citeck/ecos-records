package ru.citeck.ecos.records3.spring.config

import lombok.extern.slf4j.Slf4j
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.records2.RecordsService
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService
import ru.citeck.ecos.records2.meta.RecordsMetaService
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.querylang.QueryLangService
import ru.citeck.ecos.records2.request.rest.RestHandler
import ru.citeck.ecos.records2.rest.RemoteRecordsRestApi
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDaoAttsProvider
import ru.citeck.ecos.records3.RecordsProperties
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver

@Slf4j
@Configuration
open class RecordsServiceFactoryConfiguration : RecordsServiceFactory() {

    companion object {
        val log = KotlinLogging.logger {}
    }

    private var restApi: RemoteRecordsRestApi? = null
    private lateinit var props: RecordsProperties

    @Value("\${spring.application.name:}")
    private lateinit var springAppName: String

    override fun createRemoteRecordsResolver(): RemoteRecordsResolver? {
        val restApi = this.restApi
        return if (restApi != null) {
            if (props.gatewayMode) {
                log.info("Initialize remote records resolver in Gateway mode")
            } else {
                log.info("Initialize remote records resolver in normal mode")
            }
            val resolver = RemoteRecordsResolver(this, restApi)
            if (props.gatewayMode) {
                resolver.setDefaultAppName(props.defaultApp)
            }
            resolver
        } else {
            check(!props.gatewayMode) {
                ("restApi should be not null in gateway mode! Props: $props")
            }
            log.warn("RecordsRestConnection is not exists. Remote records requests wont be allowed")
            null
        }
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
    override fun createRecordsMetaService(): RecordsMetaService {
        return super.createRecordsMetaService()
    }

    @Bean
    override fun createPredicateService(): PredicateService {
        return super.createPredicateService()
    }

    @Bean
    override fun createMetaRecordsDaoAttsProvider(): MetaRecordsDaoAttsProvider {
        return super.createMetaRecordsDaoAttsProvider()
    }

    override fun createProperties(): RecordsProperties {
        return this.props!!
    }

    @Autowired(required = false)
    fun setConnection(restApi: RemoteRecordsRestApi?) {
        this.restApi = restApi
    }

    @Autowired
    fun setProperties(props: RecordsProperties) {
        this.props = props
        if (springAppName.isNotEmpty() && props.appName.isEmpty()) {
            props.appName = springAppName
        }
    }
}
