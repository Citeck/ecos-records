package ru.citeck.ecos.records3.spring.config

import com.netflix.discovery.EurekaClient
import mu.KotlinLogging
import org.apache.http.client.HttpClient
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContextBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import ru.citeck.ecos.records2.rest.*
import ru.citeck.ecos.records3.RecordsProperties
import ru.citeck.ecos.records3.spring.web.SkipSslVerificationHttpRequestFactory
import ru.citeck.ecos.records3.spring.web.interceptor.RecordsAuthInterceptor
import java.security.KeyStore
import javax.net.ssl.SSLContext

@Configuration
open class RecordsRestConfiguration {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private var eurekaClient: EurekaClient? = null
    private var authInterceptor: RecordsAuthInterceptor? = null

    private lateinit var properties: RecordsProperties
    private lateinit var restTemplateBuilder: RestTemplateBuilder

    @Bean
    open fun remoteRespApi(restQueryExceptionConverter: RestQueryExceptionConverter): RemoteRecordsRestApi {
        return RemoteRecordsRestApiImpl(
            { url, request -> jsonPost(url, request) },
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

        val restTemplate = if (url.startsWith("https")) {
            recordsSecureRestTemplate()
        } else {
            recordsInsecureRestTemplate()
        }

        val result = restTemplate.exchange(url, HttpMethod.POST, httpEntity, ByteArray::class.java)
        val resultEntity = RestResponseEntity()
        resultEntity.body = result.body
        resultEntity.status = result.statusCode.value()
        result.headers.forEach { k: String?, v: List<String?>? -> resultEntity.headers.put(k, v) }

        return resultEntity
    }

    @Bean
    @LoadBalanced
    open fun recordsSecureRestTemplate(): RestTemplate {

        val tlsProps = properties.tls

        if (!tlsProps.enabled) {
            log.info { "TLS is not enabled. Secure RecordsRestTemplate will be replaced by insecure." }
            return restTemplateBuilder
                .requestFactory(SkipSslVerificationHttpRequestFactory::class.java)
                .additionalInterceptors(authInterceptor)
                .build()
        }

        log.info { "SecureRestTemplate initialization started. TrustStore: ${tlsProps.trustStore}" }

        if (tlsProps.trustStore.isBlank()) {
            error("tls.enabled == true, but trustStore is not defined")
        }

        val keyStore = ClassPathResource(tlsProps.trustStore).inputStream.use {
            val keyStore = KeyStore.getInstance(tlsProps.trustStoreType)
            keyStore.load(it, tlsProps.trustStorePassword?.toCharArray())
            keyStore
        }

        val sslContext: SSLContext = SSLContextBuilder()
            .loadTrustMaterial(keyStore, null)
            .build()
        val socketFactory = SSLConnectionSocketFactory(sslContext)

        val httpClientBuilder = HttpClients.custom()
            .setSSLSocketFactory(socketFactory)

        if (!properties.tls.verifyHostname) {
            httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
        }
        val httpClient: HttpClient = httpClientBuilder.build()

        val factory = HttpComponentsClientHttpRequestFactory(httpClient)

        return this.restTemplateBuilder
            .requestFactory { factory }
            .additionalInterceptors(authInterceptor)
            .build()
    }

    @Bean
    @LoadBalanced
    open fun recordsInsecureRestTemplate(): RestTemplate {
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
