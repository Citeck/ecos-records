package ru.citeck.ecos.records2.spring.rest;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.citeck.ecos.records2.RecordsProperties;
import ru.citeck.ecos.records2.resolver.RemoteRecordsResolver;
import ru.citeck.ecos.records2.source.dao.remote.RecordsRestConnection;
import ru.citeck.ecos.records2.spring.RemoteRecordsUtils;
import ru.citeck.ecos.records2.spring.rest.interceptor.RecordsAuthInterceptor;
import ru.citeck.ecos.records2.utils.StringUtils;

import java.util.Map;

@Slf4j
@Configuration
public class RecordsRestConfig {

    public static final String RECS_BASE_URL_META_KEY = "records-base-url";
    public static final String RECS_USER_BASE_URL_META_KEY = "records-user-base-url";

    private EurekaClient eurekaClient;
    private RecordsProperties properties;
    private RestTemplateBuilder restTemplateBuilder;
    private RecordsAuthInterceptor authInterceptor;

    @Bean
    public RecordsRestConnection recordsRestConnection() {
        return this::jsonPost;
    }

    private <T> T jsonPost(String url, Object req, Class<T> respType) {
        String recordsUrl = convertUrl(url);
        try {
            return recordsRestTemplate().postForObject(recordsUrl, req, respType);
        } catch (HttpClientErrorException e) {
            log.error("Json POST failed. URL: " + recordsUrl);
            throw e;
        }
    }

    private RecordsProperties.App getAppProps(String id) {
        Map<String, RecordsProperties.App> apps = properties.getApps();
        return apps != null ? apps.get(id) : null;
    }

    private String convertUrl(String url) {

        if (eurekaClient == null) {
            return url;
        }

        String baseUrlReplacement;
        String instanceId = getInstanceId(url);

        RecordsProperties.App app = getAppProps(instanceId);

        String baseUrl = app != null ? app.getRecBaseUrl() : null;
        String userBaseUrl = app != null ? app.getRecUserBaseUrl() : null;

        if (RemoteRecordsUtils.isSystemContext()) {
            if (baseUrl != null) {
                baseUrlReplacement = baseUrl;
            } else {
                baseUrlReplacement = getEurekaMetaParam(instanceId, RECS_BASE_URL_META_KEY);
            }
        } else {
            if (userBaseUrl != null) {
                baseUrlReplacement = userBaseUrl;
            } else {
                baseUrlReplacement = getEurekaMetaParam(instanceId, RECS_USER_BASE_URL_META_KEY);
            }
        }

        if (StringUtils.isNotBlank(baseUrlReplacement)) {
            url = url.replace(RemoteRecordsResolver.BASE_URL, baseUrlReplacement);
        }

        return url;
    }

    private String getEurekaMetaParam(String instanceId, String param) {
        InstanceInfo instanceInfo = eurekaClient.getNextServerFromEureka(instanceId, false);
        return instanceInfo.getMetadata().get(param);
    }

    private String getInstanceId(String url) {
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
            .additionalInterceptors(authInterceptor)
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
    public void setAuthInterceptor(RecordsAuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }
}
