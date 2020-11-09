package ru.citeck.ecos.records3.spring.web.interceptor

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.support.BasicAuthorizationInterceptor
import org.springframework.stereotype.Component
import ru.citeck.ecos.records2.rest.RemoteRecordsUtils
import ru.citeck.ecos.records3.RecordsProperties
import java.io.IOException
import java.util.*

@Component
class RecordsAuthInterceptor @Autowired constructor(
    properties: RecordsProperties,
    cookiesAndLangInterceptor: CookiesAndLangInterceptor
) : ClientHttpRequestInterceptor {

    companion object {
        val log = KotlinLogging.logger {}
    }

    private val userRequestInterceptor: ClientHttpRequestInterceptor
    private val sysReqInterceptors: MutableMap<String, ClientHttpRequestInterceptor> = HashMap()

    init {
        userRequestInterceptor = cookiesAndLangInterceptor
        properties.apps.forEach { (id, app) ->
            val auth = app.auth
            if (auth != null) {
                sysReqInterceptors[id] = BasicAuthorizationInterceptor(auth.username, auth.password)
            }
        }
    }

    @Throws(IOException::class)
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {

        if (!RemoteRecordsUtils.isSystemContext()) {
            return userRequestInterceptor.intercept(request, body, execution)
        }
        val path = request.uri.path
        val secondSlashIdx = path.indexOf('/', 1)
        if (secondSlashIdx < 0) {
            log.warn("App id can't be extracted. URI: " + request.uri)
            return execution.execute(request, body)
        }
        val appId = path.substring(1, secondSlashIdx)
        val interceptor = sysReqInterceptors[appId]
        return if (interceptor != null) {
            interceptor.intercept(request, body, execution)
        } else {
            execution.execute(request, body)
        }
    }
}
