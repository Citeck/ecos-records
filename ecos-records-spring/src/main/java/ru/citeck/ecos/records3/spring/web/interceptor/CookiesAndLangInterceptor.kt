package ru.citeck.ecos.records3.spring.web.interceptor

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.io.IOException
import javax.servlet.http.HttpServletRequest

//Relays ALL received cookies.
//We could only relay JSESSIONID cookie - but, given how record-searching endpoint is served by Alfresco
//  and its authentication is managed by alfresco UI cookies, it's bad for out microservice to
//  know which cookies exactly are responsible for auth. On the other hand, while usually it's
//  insecure to expose all client's cookies sent to us to some another external service, in this scenario the
//  external service is not unrelated to our client, as both UI and records-search endpoints are served by alfresco.
//todo However the whole solution still smells and is temporary, until we are able check access to records
//  without asking alfresco, which is expected to be possible soon. That will allow us to just call records-service
//  with admin user or maybe microservice auth (if records-search service becomes a microservice),
//  and either post-filter returned records or specify username to test access against.
@Component
class CookiesAndLangInterceptor : ClientHttpRequestInterceptor {

    private var thisRequest: HttpServletRequest? = null

    @Throws(IOException::class)
    override fun intercept(newRequest: HttpRequest,
                           bytes: ByteArray,
                           clientHttpRequestExecution: ClientHttpRequestExecution): ClientHttpResponse {
        val newHeaders = newRequest.headers
        if (thisRequest != null) {
            newHeaders["Cookie"] = thisRequest!!.getHeader("Cookie")
            newHeaders["Accept-Language"] = thisRequest!!.getHeader("Accept-Language")
            newHeaders["X-Alfresco-Remote-User"] = thisRequest!!.getHeader("X-Alfresco-Remote-User")
            newHeaders["X-uisrv-user"] = thisRequest!!.getHeader("X-uisrv-user")
        }
        return clientHttpRequestExecution.execute(newRequest, bytes)
    }

    @Autowired(required = false)
    fun setThisRequest(thisRequest: HttpServletRequest?) {
        this.thisRequest = thisRequest
    }
}
