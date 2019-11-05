package ru.citeck.ecos.records2.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.citeck.ecos.predicate.PredicateService;
import ru.citeck.ecos.querylang.QueryLangService;
import ru.citeck.ecos.records2.RecordsProperties;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValuesConverter;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.request.rest.RestHandler;
import ru.citeck.ecos.records2.resolver.RecordsResolver;
import ru.citeck.ecos.records2.resolver.RemoteRecordsResolver;
import ru.citeck.ecos.records2.source.dao.remote.RecordsRestConnection;

@Slf4j
@Configuration
public class RecordsServiceFactoryConfig extends RecordsServiceFactory {

    private RecordsRestConnection connection;
    private RecordsProperties properties;

    @Override
    protected RemoteRecordsResolver createRemoteRecordsResolver() {
        if (connection != null) {
            return new RemoteRecordsResolver(this, connection);
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

    @Override
    protected RecordsProperties createProperties() {
        return properties;
    }

    @Autowired(required = false)
    public void setConnection(RecordsRestConnection connection) {
        this.connection = connection;
    }

    @Autowired
    public void setProperties(RecordsProperties properties) {
        this.properties = properties;
    }
}
