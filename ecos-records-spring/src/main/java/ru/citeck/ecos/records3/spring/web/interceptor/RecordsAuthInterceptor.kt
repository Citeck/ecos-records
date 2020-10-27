package ru.citeck.ecos.records3.spring.web.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordsProperties;
import ru.citeck.ecos.records2.rest.RemoteRecordsUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class RecordsAuthInterceptor implements ClientHttpRequestInterceptor {

    private final ClientHttpRequestInterceptor userRequestInterceptor;
    private final Map<String, ClientHttpRequestInterceptor> sysReqInterceptors = new HashMap<>();

    @Autowired
    public RecordsAuthInterceptor(RecordsProperties properties,
                                  CookiesAndLangInterceptor cookiesAndLangInterceptor) {

        userRequestInterceptor = cookiesAndLangInterceptor;

        Map<String, RecordsProperties.App> apps = properties.getApps();
        if (apps == null) {
            return;
        }

        apps.forEach((id, app) -> {
            RecordsProperties.Authentication auth = app.getAuth();
            if (auth != null) {
                sysReqInterceptors.put(id, new BasicAuthorizationInterceptor(auth.getUsername(), auth.getPassword()));
            }
        });
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {

        if (!RemoteRecordsUtils.isSystemContext()) {
            return userRequestInterceptor.intercept(request, body, execution);
        }

        String path = request.getURI().getPath();

        int secondSlashIdx = path.indexOf('/', 1);
        if (secondSlashIdx < 0) {
            log.warn("App id can't be extracted. URI: " + request.getURI());
            return execution.execute(request, body);
        }

        String appId = path.substring(1, secondSlashIdx);
        ClientHttpRequestInterceptor interceptor = sysReqInterceptors.get(appId);

        if (interceptor != null) {
            return interceptor.intercept(request, body, execution);
        }

        return execution.execute(request, body);
    }
}
