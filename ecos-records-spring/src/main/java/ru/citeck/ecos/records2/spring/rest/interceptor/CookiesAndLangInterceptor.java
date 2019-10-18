package ru.citeck.ecos.records2.spring.rest.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

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
public class CookiesAndLangInterceptor implements ClientHttpRequestInterceptor {

    private HttpServletRequest thisRequest;

    @Override
    public ClientHttpResponse intercept(HttpRequest newRequest,
                                        byte[] bytes,
                                        ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {

        HttpHeaders newHeaders = newRequest.getHeaders();

        if (thisRequest != null) {
            newHeaders.set("Cookie", thisRequest.getHeader("Cookie"));
            newHeaders.set("Accept-Language", thisRequest.getHeader("Accept-Language"));
        }
        return clientHttpRequestExecution.execute(newRequest, bytes);
    }

    @Autowired(required = false)
    public void setThisRequest(HttpServletRequest thisRequest) {
        this.thisRequest = thisRequest;
    }
}
