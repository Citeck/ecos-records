package ru.citeck.ecos.records2.rest;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.context.lib.auth.AuthContext;
import ru.citeck.ecos.records3.RecordsProperties;
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class RemoteRecordsRestApiImpl implements RemoteRecordsRestApi {

    private static final String HTTPS_PREFIX = "https:/";
    private static final String HTTP_PREFIX = "http:/";

    private static final Pattern APP_NAME_PATTERN = Pattern.compile("^https?://(.+?)/.*");

    private final RecordsRestTemplate template;
    private final RecordsProperties properties;
    private final RemoteAppInfoProvider remoteAppInfoProvider;
    private final RestQueryExceptionConverter restQueryExceptionConverter;

    private final Map<String, RecordsProperties.App> appProps = new ConcurrentHashMap<>();

    public RemoteRecordsRestApiImpl(RecordsRestTemplate template,
                                    RemoteAppInfoProvider remoteAppInfoProvider,
                                    RecordsProperties properties,
                                    RestQueryExceptionConverter restQueryExceptionConverter) {
        this.template = template;
        this.properties = properties;
        this.remoteAppInfoProvider = remoteAppInfoProvider;
        this.restQueryExceptionConverter = restQueryExceptionConverter;
    }

    public RemoteRecordsRestApiImpl(RecordsRestTemplate template,
                                    RemoteAppInfoProvider remoteAppInfoProvider,
                                    RecordsProperties properties) {
        this.template = template;
        this.properties = properties;
        this.remoteAppInfoProvider = remoteAppInfoProvider;
        this.restQueryExceptionConverter = new RestQueryExceptionConverterDefault();
    }

    public <T> T jsonPost(String url, Object request, Class<T> respType) {

        String recordsUrl = convertUrl(url);

        RestRequestEntity requestEntity = new RestRequestEntity();
        requestEntity.setBody(Json.getMapper().toBytes(request));

        HttpHeaders headers = new HttpHeaders();
        headers.put(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        requestEntity.setHeaders(headers);

        RestResponseEntity responseEntity;

        try {
            responseEntity = template.jsonPost(recordsUrl, requestEntity);
        } catch (Exception e) {
            logAndThrowError(recordsUrl, requestEntity, null, e);
            throw new RuntimeException(e);
        }

        if (responseEntity.getStatus() != 200) {
            logAndThrowError(recordsUrl, requestEntity, responseEntity, null);
        }

        return Json.getMapper().read(responseEntity.getBody(), respType);
    }

    private void logAndThrowError(String url, RestRequestEntity request, RestResponseEntity response, Exception e) {

        String targetAppHost = "-";
        String targetAppIp = "-";

        String appName = getAppName(url);

        try {
            RemoteAppInfo appInfo = remoteAppInfoProvider.getAppInfo(appName);
            if (appInfo != null) {
                targetAppHost = appInfo.getHost() + ":" + appInfo.getPort();
                targetAppIp = appInfo.getIp() + ":" + appInfo.getPort();
            }
        } catch (Exception appInfoResolveException) {
            log.warn("Application info can't be received: '" + appName + "'", appInfoResolveException);
        }

        int status = response != null ? response.getStatus() : -1;

        String msg = "Json POST failed. Host: '" + targetAppHost + "' IP: '"
                     + targetAppIp + "' URL: " + url + " Status code: " + status;

        if (e != null) {
            log.error(msg, e);
        } else {
            log.error(msg);
        }

        logErrorObject("Request body", request != null ? request.getBody() : null);
        logErrorObject("Request resp", response != null ? response.getBody() : null);

        throw restQueryExceptionConverter.convert(msg, e);
    }

    private void logErrorObject(String prefix, Object obj) {
        String str;
        if (obj == null) {
            str = "null";
        } else if (obj instanceof byte[]) {
            str = new String((byte[]) obj, StandardCharsets.UTF_8);
        } else {
            try {
                str = Json.getMapper().toString(obj);
            } catch (Exception e) {
                log.error("log conversion failed: " + e.getClass() + " " + e.getMessage());
                try {
                    str = obj.toString();
                } catch (Exception ex) {
                    log.error("log toString failed: " + ex.getClass() + " " + ex.getMessage());
                    str = obj.getClass() + "@" + System.identityHashCode(obj);
                }
            }
        }
        log.error(prefix + ": " + str);
    }

    private RecordsProperties.App getAppProps(String id) {
        return appProps.computeIfAbsent(id, appId -> {
            RecordsProperties.App appProps = properties.getApps().get(appId);
            if (appProps == null) {
                appProps = new RecordsProperties.App();
            }
            return appProps;
        });
    }

    private String convertUrl(String url) {

        String appName = url.substring(1, url.indexOf('/', 1));
        RecordsProperties.App appProps = getAppProps(appName);

        if (remoteAppInfoProvider == null) {
            return HTTP_PREFIX + url;
        }

        String baseUrlReplacement;

        RemoteAppInfo appInfo = null;
        try {
            appInfo = remoteAppInfoProvider.getAppInfo(appName);
        } catch (Exception e) {
            log.error("App info can't be resolved for '" + appName + "'. " +
                "Exception type: '" + e.getClass() + "' msg: '" + e.getMessage() + "'");
        }
        if (appInfo == null) {
            appInfo = RemoteAppInfo.EMPTY;
        }

        String schema;
        if (Boolean.TRUE.equals(appInfo.getSecurePortEnabled())) {
            schema = HTTPS_PREFIX;
        } else {
            schema = HTTP_PREFIX;
        }
        url = schema + url;

        String baseUrl = appProps.getRecBaseUrl();
        String userBaseUrl = appProps.getRecUserBaseUrl();

        if (AuthContext.isRunAsSystem()) {
            if (StringUtils.isNotBlank(baseUrl)) {
                baseUrlReplacement = baseUrl;
            } else {
                baseUrlReplacement = appInfo.getRecordsBaseUrl();
            }
        } else {
            if (StringUtils.isNotBlank(userBaseUrl)) {
                baseUrlReplacement = userBaseUrl;
            } else {
                baseUrlReplacement = appInfo.getRecordsUserBaseUrl();
            }
        }

        if (StringUtils.isNotBlank(baseUrlReplacement)) {
            url = url.replace(RemoteRecordsResolver.BASE_URL, baseUrlReplacement);
        }

        return url;
    }

    private String getAppName(String url) {
        Matcher matcher = APP_NAME_PATTERN.matcher(url);
        if (!matcher.matches()) {
            return "";
        }
        return matcher.group(1);
    }
}
