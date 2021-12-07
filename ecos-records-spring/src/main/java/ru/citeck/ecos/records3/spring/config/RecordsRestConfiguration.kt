package ru.citeck.ecos.records3.spring.config

import com.netflix.appinfo.InstanceInfo
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
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.util.ResourceUtils
import org.springframework.web.client.RestTemplate
import ru.citeck.ecos.records2.rest.*
import ru.citeck.ecos.records3.RecordsProperties
import ru.citeck.ecos.records3.spring.web.SkipSslVerificationHttpRequestFactory
import ru.citeck.ecos.records3.spring.web.interceptor.RecordsAuthInterceptor
import java.security.KeyStore

@Configuration
open class RecordsRestConfiguration {

    companion object {
        private val log = KotlinLogging.logger {}

        private const val TRUST_STORE_NAME = "TrustStore"
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

        return object : RemoteAppInfoProvider {
            override fun getAppInfo(appName: String): RemoteAppInfo? {

                val eureka = this@RecordsRestConfiguration.eurekaClient ?: return null

                val instanceInfo = try {
                    eureka.getNextServerFromEureka(appName, true)
                } catch (e: Exception) {
                    log.debug(e) { "Secure app doesn't found: $appName" }
                    eureka.getNextServerFromEureka(appName, false)
                }

                return RemoteAppInfo.create()
                    .withIp(instanceInfo.ipAddr)
                    .withPort(instanceInfo.port)
                    .withHost(instanceInfo.hostName)
                    .withRecordsBaseUrl(instanceInfo.metadata[RestConstants.RECS_BASE_URL_META_KEY])
                    .withRecordsUserBaseUrl(instanceInfo.metadata[RestConstants.RECS_USER_BASE_URL_META_KEY])
                    .withSecurePortEnabled(instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE))
                    .build()
            }
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

    private fun logTlsInfo(msg: () -> String) {
        log.info { "[Records TLS] ${msg.invoke()}" }
    }

    @Bean
    @LoadBalanced
    open fun recordsSecureRestTemplate(): RestTemplate {

        val tlsProps = properties.tls

        if (!tlsProps.enabled) {
            logTlsInfo { "TLS disabled. Secure SecureRestTemplate will be replaced by insecure." }
            return restTemplateBuilder
                .requestFactory(SkipSslVerificationHttpRequestFactory::class.java)
                .additionalInterceptors(authInterceptor)
                .build()
        }

        logTlsInfo { "TLS enabled. SecureRestTemplate initialization started." }

        val sslContextBuilder = SSLContextBuilder()

        if (tlsProps.trustStore.isNotBlank()) {

            val trustStore = loadKeyStore(
                TRUST_STORE_NAME,
                tlsProps.trustStore,
                tlsProps.trustStorePassword,
                tlsProps.trustStoreType
            )

            sslContextBuilder.loadTrustMaterial(trustStore, null)
        } else {

            logTlsInfo { "Custom $TRUST_STORE_NAME doesn't defined. Default will be used." }
        }

        var hostnameVerifier = SSLConnectionSocketFactory.getDefaultHostnameVerifier()
        if (!properties.tls.verifyHostname) {
            logTlsInfo { "Hostname verification is disabled" }
            hostnameVerifier = NoopHostnameVerifier.INSTANCE
        } else {
            logTlsInfo { "Hostname verification is enabled" }
        }

        val socketFactory = SSLConnectionSocketFactory(sslContextBuilder.build(), hostnameVerifier)

        val httpClient: HttpClient = HttpClients.custom()
            .setSSLSocketFactory(socketFactory)
            .setSSLHostnameVerifier(hostnameVerifier)
            .build()

        val factory = HttpComponentsClientHttpRequestFactory(httpClient)

        return this.restTemplateBuilder
            .requestFactory { factory }
            .additionalInterceptors(authInterceptor)
            .build()
    }

    private fun loadKeyStore(name: String, path: String, password: String?, type: String): KeyStore {

        logTlsInfo { "Start loading $name with type $type by path: $path" }
        val url = ResourceUtils.getURL(path)
        logTlsInfo { "$name URL: $url" }

        return url.openStream().use {
            val keyStore = KeyStore.getInstance(type)
            keyStore.load(it, password?.toCharArray())
            logTlsInfo { "$name loading finished. Entries size: ${keyStore.size()}" }
            keyStore
        }
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
