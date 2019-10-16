package ru.citeck.ecos.records2.spring.rest;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.citeck.ecos.records2.resolver.RemoteRecordsResolver;
import ru.citeck.ecos.records2.source.dao.remote.RecordsRestConnection;
import ru.citeck.ecos.records2.spring.RecordsProperties;
import ru.citeck.ecos.records2.spring.rest.interceptor.RecordsAlfrescoAuthInterceptor;
import ru.citeck.ecos.records2.utils.StringUtils;

@Configuration
public class RecordsRestConfig {

    private EurekaClient eurekaClient;
    private RecordsProperties properties;
    private RestTemplateBuilder restTemplateBuilder;
    private RecordsAlfrescoAuthInterceptor alfrescoAuthInterceptor;

    @Bean
    public RecordsRestConnection recordsRestConnection() {
        return this::jsonPost;
    }

    private <T> T jsonPost(String url, Object req, Class<T> respType) {
        String serverId = getServerId(url);

        if (eurekaClient != null) {
            InstanceInfo instanceInfo = eurekaClient.getNextServerFromEureka(serverId, false);
            String apiUrl = instanceInfo.getMetadata().get("records-base-url");
            if (StringUtils.isNotBlank(apiUrl)) {
                url = url.replace(RemoteRecordsResolver.BASE_URL, apiUrl);
            }
        }

        return recordsRestTemplate().postForObject(url, req, respType);
    }

    private String getServerId(String url) {
        int firstSlashIndex = url.indexOf("/");
        int nextSlashIndex = url.indexOf("/", firstSlashIndex + 1);
        return url.substring(firstSlashIndex + 1, nextSlashIndex);
    }

    @Bean
    @LoadBalanced
    public RestTemplate recordsRestTemplate() {

        RecordsProperties.RestProps microRest = properties.getRest();
        String rootUri = microRest != null && Boolean.TRUE.equals(microRest.getSecure()) ? "https:/" : "http:/";

        return restTemplateBuilder
            .requestFactory(SkipSslVerificationHttpRequestFactory.class)
            .additionalInterceptors(alfrescoAuthInterceptor)
            .rootUri(rootUri)
            .build();
    }

    @Autowired(required = false)
    public void setRestTemplateBuilder(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplateBuilder = restTemplateBuilder;
    }

    @Autowired(required = false)
    public void setEurekaClient(EurekaClient eurekaClient) {
        this.eurekaClient = eurekaClient;
    }

    @Autowired
    public void setProperties(RecordsProperties properties) {
        this.properties = properties;
    }

    @Autowired
    public void setAlfrescoAuthInterceptor(RecordsAlfrescoAuthInterceptor alfrescoAuthInterceptor) {
        this.alfrescoAuthInterceptor = alfrescoAuthInterceptor;
    }
}
