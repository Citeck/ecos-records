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
class CookiesAndLangInterceptor : ClientHttpRequestInterceptor {

    private var thisRequest: HttpServletRequest? = null

    @Throws(IOException::class)
    override fun intercept(
        newRequest: HttpRequest,
        bytes: ByteArray,
        clientHttpRequestExecution: ClientHttpRequestExecution
    ): ClientHttpResponse {

        val newHeaders = newRequest.headers
        val request = thisRequest
        if (request != null) {
            newHeaders["Cookie"] = request.getHeader("Cookie")
            newHeaders["Accept-Language"] = request.getHeader("Accept-Language")
            newHeaders["X-Alfresco-Remote-User"] = request.getHeader("X-Alfresco-Remote-User")
            newHeaders["X-ECOS-User"] = request.getHeader("X-ECOS-User")
        }
        return clientHttpRequestExecution.execute(newRequest, bytes)
    }

    @Autowired(required = false)
    fun setThisRequest(thisRequest: HttpServletRequest?) {
        this.thisRequest = thisRequest
    }
}
