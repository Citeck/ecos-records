package ru.citeck.ecos.records3.spring.web.interceptor

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.io.IOException
import javax.servlet.http.HttpServletRequest

@Component
class AuthHeaderInterceptor : ClientHttpRequestInterceptor {

    companion object {
        const val AUTH_HEADER = "Authorization"
    }

    private var authHeaderProvider: AuthHeaderProvider? = null
    private var thisRequest: HttpServletRequest? = null

    @Throws(IOException::class)
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {

        val currentRequest = thisRequest
        if (currentRequest != null) {
            request.headers.set(AUTH_HEADER, currentRequest.getHeader(AUTH_HEADER))
        }

        val authHeaderProvider = authHeaderProvider ?: return execution.execute(request, body)

        val userName = request.headers.getFirst("X-ECOS-User")
        if (userName.isNullOrBlank()) {
            return execution.execute(request, body)
        }

        val header = authHeaderProvider.getAuthHeader(userName)
        if (header.isNullOrBlank()) {
            request.headers.remove(AUTH_HEADER)
        } else {
            request.headers.set(AUTH_HEADER, header)
        }

        return execution.execute(request, body)
    }

    fun setAuthHeaderProvider(authHeaderProvider: AuthHeaderProvider) {
        this.authHeaderProvider = authHeaderProvider
    }

    @Autowired(required = false)
    fun setThisRequest(thisRequest: HttpServletRequest?) {
        this.thisRequest = thisRequest
    }
}
