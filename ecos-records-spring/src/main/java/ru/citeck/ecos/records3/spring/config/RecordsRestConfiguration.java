package ru.citeck.ecos.records3.spring.config;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import ru.citeck.ecos.records3.RecordsProperties;
import ru.citeck.ecos.records3.rest.*;
import ru.citeck.ecos.records3.spring.web.SkipSslVerificationHttpRequestFactory;
import ru.citeck.ecos.records3.spring.web.interceptor.RecordsAuthInterceptor;

@Slf4j
@Configuration
public class RecordsRestConfiguration {

    private EurekaClient eurekaClient;
    private RecordsProperties properties;
    private RestTemplateBuilder restTemplateBuilder;
    private RecordsAuthInterceptor authInterceptor;

    @Bean
    public RemoteRecordsRestApi remoteRespApi(RestQueryExceptionConverter restQueryExceptionConverter) {
        return new RemoteRecordsRestApiImpl(this::jsonPost, createRemoteAppInfoProvider(), properties,
            restQueryExceptionConverter);
    }

    private RemoteAppInfoProvider createRemoteAppInfoProvider() {

        return appName -> {

            RemoteAppInfo info = new RemoteAppInfo();

            InstanceInfo instanceInfo = eurekaClient.getNextServerFromEureka(appName, false);
            info.setIp(instanceInfo.getIPAddr());
            info.setPort(instanceInfo.getPort());
            info.setHost(instanceInfo.getHostName());

            info.setRecordsBaseUrl(instanceInfo.getMetadata().get(RestConstants.RECS_BASE_URL_META_KEY));
            info.setRecordsUserBaseUrl(instanceInfo.getMetadata().get(RestConstants.RECS_USER_BASE_URL_META_KEY));

            return info;
        };
    }

    private RestResponseEntity jsonPost(String url, RestRequestEntity request) {

        HttpHeaders headers = new HttpHeaders();
        request.getHeaders().forEach(headers::put);
        HttpEntity<byte[]> httpEntity = new HttpEntity<>(request.getBody(), headers);

        ResponseEntity<byte[]> result = recordsRestTemplate().exchange(url, HttpMethod.POST, httpEntity, byte[].class);

        RestResponseEntity resultEntity = new RestResponseEntity();
        resultEntity.setBody(result.getBody());
        resultEntity.setStatus(result.getStatusCode().value());
        result.getHeaders().forEach((k, v) -> resultEntity.getHeaders().put(k, v));

        return resultEntity;
    }

    @Bean
    @LoadBalanced
    public RestTemplate recordsRestTemplate() {

        return restTemplateBuilder
            .requestFactory(SkipSslVerificationHttpRequestFactory.class)
            .additionalInterceptors(authInterceptor)
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
