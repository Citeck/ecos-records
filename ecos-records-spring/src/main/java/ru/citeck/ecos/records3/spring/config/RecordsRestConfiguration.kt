package ru.citeck.ecos.records3.spring.config

import com.netflix.discovery.EurekaClient
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import ru.citeck.ecos.records2.RecordsProperties
import ru.citeck.ecos.records2.rest.*
import ru.citeck.ecos.records3.spring.web.SkipSslVerificationHttpRequestFactory
import ru.citeck.ecos.records3.spring.web.interceptor.RecordsAuthInterceptor

@Slf4j
@Configuration
open class RecordsRestConfiguration {

    private var eurekaClient: EurekaClient? = null
    private var properties: RecordsProperties? = null
    private var authInterceptor: RecordsAuthInterceptor? = null
    private lateinit var restTemplateBuilder: RestTemplateBuilder

    @Bean
    open fun remoteRespApi(restQueryExceptionConverter: RestQueryExceptionConverter): RemoteRecordsRestApi {
        return RemoteRecordsRestApiImpl(
            RecordsRestTemplate {
                url, request ->
                jsonPost(url, request)
            },
            createRemoteAppInfoProvider(), properties,
            restQueryExceptionConverter
        )
    }

    private fun createRemoteAppInfoProvider(): RemoteAppInfoProvider {
        return RemoteAppInfoProvider { appName: String? ->
            val info = RemoteAppInfo()
            val instanceInfo = eurekaClient!!.getNextServerFromEureka(appName, false)
            info.ip = instanceInfo.ipAddr
            info.port = instanceInfo.port
            info.host = instanceInfo.hostName
            info.recordsBaseUrl = instanceInfo.metadata[RestConstants.RECS_BASE_URL_META_KEY]
            info.recordsUserBaseUrl = instanceInfo.metadata[RestConstants.RECS_USER_BASE_URL_META_KEY]
            info
        }
    }

    private fun jsonPost(url: String, request: RestRequestEntity): RestResponseEntity {
        val headers = HttpHeaders()
        request.headers.forEach { key: String?, value: List<String?>? -> headers[key] = value }
        val httpEntity = HttpEntity(request.body, headers)
        val result = recordsRestTemplate().exchange(url, HttpMethod.POST, httpEntity, ByteArray::class.java)
        val resultEntity = RestResponseEntity()
        resultEntity.body = result.body
        resultEntity.status = result.statusCode.value()
        result.headers.forEach { k: String?, v: List<String?>? -> resultEntity.headers.put(k, v) }
        return resultEntity
    }

    @Bean
    @LoadBalanced
    open fun recordsRestTemplate(): RestTemplate {
        return restTemplateBuilder
            .requestFactory(SkipSslVerificationHttpRequestFactory::class.java)
            .additionalInterceptors(authInterceptor)
            .build()
    }

    @Autowired(required = false)
    fun setRestTemplateBuilder(restTemplateBuilder: RestTemplateBuilder) {
        this.restTemplateBuilder = restTemplateBuilder
    }

    @Autowired(required = false)
    fun setEurekaClient(eurekaClient: EurekaClient) {
        this.eurekaClient = eurekaClient
    }

    @Autowired
    fun setProperties(properties: RecordsProperties) {
        this.properties = properties
    }

    @Autowired
    fun setAuthInterceptor(authInterceptor: RecordsAuthInterceptor) {
        this.authInterceptor = authInterceptor
    }
}
