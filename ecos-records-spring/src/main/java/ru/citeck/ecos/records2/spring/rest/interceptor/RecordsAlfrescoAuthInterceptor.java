package ru.citeck.ecos.records2.spring.rest.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.spring.RecordsProperties;
import ru.citeck.ecos.records2.spring.RemoteRecordsUtils;

import java.io.IOException;

@Component
public class RecordsAlfrescoAuthInterceptor implements ClientHttpRequestInterceptor {

    private ClientHttpRequestInterceptor alfRunAsSystemInterceptor;
    private ClientHttpRequestInterceptor alfUserRequestInterceptor;

    @Autowired
    public RecordsAlfrescoAuthInterceptor(RecordsProperties properties,
                                          CookiesAndLangInterceptor cookiesAndLangInterceptor) {

        RecordsProperties.AlfProps alfresco = properties.getAlfresco();
        if (alfresco == null) {
            return;
        }
        RecordsProperties.Authentication auth = alfresco.getAuth();
        if (auth == null) {
            return;
        }
        alfRunAsSystemInterceptor = new BasicAuthorizationInterceptor(auth.getUsername(), auth.getPassword());
        alfUserRequestInterceptor = cookiesAndLangInterceptor;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {


        if (request.getURI().getPath().contains("/alfresco")) {

            if (RemoteRecordsUtils.isSystemContext() && alfRunAsSystemInterceptor != null) {
                return alfRunAsSystemInterceptor.intercept(request, body, execution);
            }

            if (alfUserRequestInterceptor != null) {
                return alfUserRequestInterceptor.intercept(request, body, execution);
            }
        }
        return execution.execute(request, body);
    }
}
