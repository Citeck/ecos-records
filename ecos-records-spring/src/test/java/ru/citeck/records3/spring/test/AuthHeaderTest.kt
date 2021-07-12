package ru.citeck.records3.spring.test

import com.netflix.appinfo.InstanceInfo
import com.netflix.discovery.EurekaClient
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.core.IsEqual
import org.hamcrest.core.IsNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestTemplate
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.rest.RemoteRecordsUtils
import ru.citeck.ecos.records3.RecordsProperties
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.context.SystemContextUtil
import ru.citeck.ecos.records3.rest.v1.mutate.MutateResp
import ru.citeck.ecos.records3.spring.web.interceptor.AuthHeaderProvider
import ru.citeck.ecos.records3.spring.web.interceptor.RecordsAuthInterceptor
import java.net.URI

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [TestApp::class])
@Import(AuthHeaderTest.TestConfig::class)
open class AuthHeaderTest {

    @Autowired
    lateinit var recordsService: RecordsService
    @Autowired
    lateinit var recordsServiceFactory: RecordsServiceFactory

    @Autowired
    @Qualifier("recordsRestTemplate")
    lateinit var restTemplate: RestTemplate
    lateinit var mockServer: MockRestServiceServer
    @MockBean
    lateinit var eurekaClient: EurekaClient
    @Autowired
    lateinit var recordsAuthInterceptor: RecordsAuthInterceptor

    private val refToMutate = RecordRef.create("test-app", "sourceId", "")
    private val resultRef = RecordRef.create("test-app", "sourceId", "newId")

    private val testAppMutUri = URI("http://test-app/api/records/mutate")

    private val mutResp = MutateResp().apply {
        setRecords(listOf(RecordAtts(resultRef)))
    }

    private val remoteServers = mapOf(
        "test-app" to InstanceInfo.Builder
            .newBuilder()
            .setIPAddr("127.0.0.1")
            .setHostName("127.0.0.1")
            .setPort(1234)
            .setAppName("test-app")
            .build(),
        "appWithCustomSystemUser" to InstanceInfo.Builder
            .newBuilder()
            .setIPAddr("127.0.0.1")
            .setHostName("127.0.0.1")
            .setPort(1234)
            .setAppName("appWithCustomSystemUser")
            .build()
    )

    @BeforeEach
    open fun init() {
        mockServer = MockRestServiceServer.createServer(restTemplate)
        Mockito.`when`(
            eurekaClient.getNextServerFromEureka(
                Mockito.anyString(),
                Mockito.anyBoolean()
            )
        ).thenAnswer { ans ->
            remoteServers[ans.arguments[0]]
        }
        recordsAuthInterceptor.setAuthHeaderProvider(object : AuthHeaderProvider {
            override fun getAuthHeader(userName: String): String = "user-$userName"
            override fun getSystemAuthHeader(userName: String) = "system-$userName"
        })
    }

    @Test
    fun test() {
        assertThat(recordsService).isNotNull

        mockServer.expect(ExpectedCount.once(), requestTo(testAppMutUri))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", IsNull()))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Json.mapper.toString(mutResp)!!)
            )

        val mutRes = recordsService.mutate(refToMutate, mapOf("one" to "Two"))

        mockServer.verify()
        assertThat(mutRes).isEqualTo(resultRef)

        testAuthHeader(null, "test-app")

        val request = MockHttpServletRequest()
        request.addHeader("X-ECOS-User", "someUser")
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        testAuthHeader("user-someUser", "test-app")
        RemoteRecordsUtils.runAsSystem {
            testAuthHeader("system-system", "test-app")
            testAuthHeader("system-customSystemUser", "appWithCustomSystemUser")
        }
        RequestContext.doWithCtx(recordsServiceFactory) {
            SystemContextUtil.doAsSystem({
                testAuthHeader("system-system", "test-app")
                testAuthHeader("system-customSystemUser", "appWithCustomSystemUser")
            })
        }
    }

    private fun testAuthHeader(expected: String?, appName: String) {

        val expectedHeader = if (expected == null) {
            IsNull<String>()
        } else {
            IsEqual(expected)
        }

        mockServer.reset()

        mockServer.expect(ExpectedCount.once(), requestTo("http://$appName/api/records/mutate"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", expectedHeader))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Json.mapper.toString(mutResp)!!)
            )

        recordsService.mutate(RecordRef.create(appName, "sourceId", "id"), mapOf("one" to "Two"))

        mockServer.verify()
    }

    @TestConfiguration
    open class TestConfig {

        @Bean
        open fun properties(): RecordsProperties {
            val props = RecordsProperties()
            props.apps = mapOf(
                "appWithCustomSystemUser" to RecordsProperties.App().apply {
                    auth = RecordsProperties.Authentication().apply {
                        username = "customSystemUser"
                    }
                }
            )
            return props
        }
    }
}
