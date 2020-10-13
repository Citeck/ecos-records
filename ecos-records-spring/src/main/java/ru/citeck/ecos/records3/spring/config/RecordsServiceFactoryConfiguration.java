package ru.citeck.ecos.records3.spring.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.citeck.ecos.records3.RecordsProperties;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.records3.graphql.meta.value.MetaValuesConverter;
import ru.citeck.ecos.records3.record.op.atts.RecordsMetaService;
import ru.citeck.ecos.records3.predicate.PredicateService;
import ru.citeck.ecos.records3.record.op.query.lang.QueryLangService;
import ru.citeck.ecos.records3.rest.RestHandler;
import ru.citeck.ecos.records3.record.resolver.RecordsResolver;
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver;
import ru.citeck.ecos.records3.rest.RemoteRecordsRestApi;
import ru.citeck.ecos.records3.source.dao.local.meta.MetaRecordsDaoAttsProvider;

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
            return new RemoteRecordsResolver(this, restApi);
        } else {
            log.warn("RecordsRestConnection is not exists. Remote records requests wont be allowed");
            return null;
        }
    }

    @Override
    protected RecordsResolver createRecordsResolver() {

        if (properties.isGatewayMode()) {

            log.info("Initialize records resolver in Gateway mode");

            RemoteRecordsResolver resolver = createRemoteRecordsResolver();
            if (resolver == null) {
                throw new IllegalStateException("RemoteRecordsResolver should "
                                                + "be not null in gateway mode! Props: " + properties);
            }
            resolver.setDefaultAppName(properties.getDefaultApp());
            return resolver;
        } else {
            return super.createRecordsResolver();
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
    protected MetaValuesConverter createMetaValuesConverter() {
        return super.createMetaValuesConverter();
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
