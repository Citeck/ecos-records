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
        if (thisRequest != null) {
            newHeaders["Cookie"] = thisRequest!!.getHeader("Cookie")
            newHeaders["Accept-Language"] = thisRequest!!.getHeader("Accept-Language")
            newHeaders["X-Alfresco-Remote-User"] = thisRequest!!.getHeader("X-Alfresco-Remote-User")
            newHeaders["X-ECOS-User"] = thisRequest!!.getHeader("X-ECOS-User")
        }
        return clientHttpRequestExecution.execute(newRequest, bytes)
    }

    @Autowired(required = false)
    fun setThisRequest(thisRequest: HttpServletRequest?) {
        this.thisRequest = thisRequest
    }
}
