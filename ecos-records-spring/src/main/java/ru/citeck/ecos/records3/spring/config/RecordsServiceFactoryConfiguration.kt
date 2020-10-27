package ru.citeck.ecos.records3.spring.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.citeck.ecos.records2.RecordsProperties;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValuesConverter;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.querylang.QueryLangService;
import ru.citeck.ecos.records2.request.rest.RestHandler;
import ru.citeck.ecos.records2.rest.RemoteRecordsRestApi;
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDaoAttsProvider;
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver;

@Slf4j
@Configuration
public class RecordsServiceFactoryConfiguration extends RecordsServiceFactory {

    private RemoteRecordsRestApi restApi;
    private RecordsProperties properties;

    @Value("${spring.application.name:}")
    private String springAppName;

    @Override
    protected RemoteRecordsResolver createRemoteRecordsResolver() {

        if (restApi != null) {

            if (properties.isGatewayMode()) {
                log.info("Initialize remote records resolver in Gateway mode");
            } else {
                log.info("Initialize remote records resolver in normal mode");
            }

            RemoteRecordsResolver resolver = new RemoteRecordsResolver(this, restApi);
            if (properties.isGatewayMode()) {
                resolver.setDefaultAppName(properties.getDefaultApp());
            }
            return resolver;

        } else {
            if (properties.isGatewayMode()) {
                throw new IllegalStateException("restApi should "
                    + "be not null in gateway mode! Props: " + properties);
            }
            log.warn("RecordsRestConnection is not exists. Remote records requests wont be allowed");
            return null;
        }
    }

    @Bean
    @Override
    protected RecordEvaluatorService createRecordEvaluatorService() {
        return super.createRecordEvaluatorService();
    }

    @Bean
    @Override
    protected RestHandler createRestHandler() {
        return super.createRestHandler();
    }

    @Bean
    @Override
    protected RecordsService createRecordsService() {
        return super.createRecordsService();
    }

    @Bean
    @Override
    protected QueryLangService createQueryLangService() {
        return super.createQueryLangService();
    }

    @Bean
    @Override
    protected RecordsMetaService createRecordsMetaService() {
        return super.createRecordsMetaService();
    }

    @Bean
    @Override
    protected PredicateService createPredicateService() {
        return super.createPredicateService();
    }

    @Bean
    @Override
    protected MetaRecordsDaoAttsProvider createMetaRecordsDaoAttsProvider() {
        return super.createMetaRecordsDaoAttsProvider();
    }

    @Override
    protected RecordsProperties createProperties() {
        return properties;
    }

    @Autowired(required = false)
    public void setConnection(RemoteRecordsRestApi restApi) {
        this.restApi = restApi;
    }

    @Autowired
    public void setProperties(RecordsProperties properties) {
        this.properties = properties;
        if (!springAppName.isEmpty() && properties.getAppName().isEmpty()) {
            properties.setAppName(springAppName);
        }
    }
}
