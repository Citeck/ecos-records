package ru.citeck.ecos.records3.spring.web.interceptor

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.support.BasicAuthorizationInterceptor
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import ru.citeck.ecos.records2.rest.RemoteRecordsUtils
import ru.citeck.ecos.records3.RecordsProperties
import java.io.IOException
import javax.servlet.http.HttpServletRequest
import kotlin.collections.HashMap

@Component
class RecordsAuthInterceptor @Autowired constructor(
    properties: RecordsProperties,
    cookiesAndLangInterceptor: CookiesAndLangInterceptor
) : ClientHttpRequestInterceptor {

    companion object {
        val log = KotlinLogging.logger {}

        private const val AUTH_HEADER = "Authorization"
        private const val ECOS_USER_HEADER = "X-ECOS-User"
        private const val ALF_USER_HEADER = "X-Alfresco-Remote-User"
    }

    private val userRequestInterceptor: ClientHttpRequestInterceptor
    private val sysAuthInterceptors: MutableMap<String, ClientHttpRequestInterceptor> = HashMap()
    private val sysUserByApp: MutableMap<String, String> = HashMap()

    private var authHeaderProvider: AuthHeaderProvider? = null

    init {
        userRequestInterceptor = cookiesAndLangInterceptor
        properties.apps.forEach { (id, app) ->
            val auth = app.auth
            val userName = auth?.username
            if (!userName.isNullOrBlank()) {
                sysUserByApp[id] = userName
                sysAuthInterceptors[id] = BasicAuthorizationInterceptor(userName, auth.password)
            }
        }
    }

    @Throws(IOException::class)
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {

        val currentRequest = getCurrentRequest()
        if (currentRequest != null) {
            request.headers.set(AUTH_HEADER, currentRequest.getHeader(AUTH_HEADER))
        }

        if (!RemoteRecordsUtils.isSystemContext()) {
            setAuthHeader(currentRequest?.getHeader(ECOS_USER_HEADER), false, request)
            return userRequestInterceptor.intercept(request, body, execution)
        }

        val appId = getAppId(request)
        if (appId.isBlank()) {
            log.warn("App id can't be extracted. URI: " + request.uri)
            return execution.execute(request, body)
        }

        val userName = sysUserByApp[appId] ?: "system"
        request.headers.add(ECOS_USER_HEADER, userName)
        request.headers.add(ALF_USER_HEADER, userName)

        if (!setAuthHeader(userName, true, request)) {
            sysAuthInterceptors[appId]?.let {
                return it.intercept(request, body, execution)
            }
        }
        return execution.execute(request, body)
    }

    private fun getAppId(request: HttpRequest): String {
        val path = request.uri.path
        return if (path.startsWith("/api/")) {
            request.uri.host
        } else {
            val secondSlashIdx = path.indexOf('/', 1)
            if (secondSlashIdx < 0) {
                return ""
            }
            path.substring(1, secondSlashIdx)
        }
    }

    private fun setAuthHeader(userName: String?, isSystem: Boolean, request: HttpRequest): Boolean {
        if (userName.isNullOrBlank()) {
            return false
        }
        val authProvider = authHeaderProvider ?: return false
        val authHeader = if (isSystem) {
            authProvider.getSystemAuthHeader(userName)
        } else {
            authProvider.getAuthHeader(userName)
        }
        if (!authHeader.isNullOrBlank()) {
            request.headers.set(AUTH_HEADER, authHeader)
            return true
        }
        return false
    }

    private fun getCurrentRequest(): HttpServletRequest? {
        return (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
    }

    fun setAuthHeaderProvider(authHeaderProvider: AuthHeaderProvider) {
        this.authHeaderProvider = authHeaderProvider
    }
}
