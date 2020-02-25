package ru.citeck.ecos.records2.rest;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordsProperties;
import ru.citeck.ecos.records2.resolver.RemoteRecordsResolver;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class RestApi {

    private RecordsRestTemplate template;
    private RecordsProperties properties;
    private RemoteAppInfoProvider remoteAppInfoProvider;

    public RestApi(RecordsRestTemplate template,
                   RemoteAppInfoProvider remoteAppInfoProvider,
                   RecordsProperties properties) {

        this.template = template;
        this.properties = properties;
        this.remoteAppInfoProvider = remoteAppInfoProvider;
    }

    public <T> T jsonPost(String url, Object request, Class<T> respType) {

        String recordsUrl = convertUrl(url);

        RestRequestEntity requestEntity = new RestRequestEntity();
        requestEntity.setBody(Json.getMapper().toBytes(request));

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
                targetAppIp = appInfo.getIp() + ":" + appInfo.getIp();
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

        if (e != null) {
            throw new RestQueryException(msg, e);
        } else {
            throw new RestQueryException(msg);
        }
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
        Map<String, RecordsProperties.App> apps = properties.getApps();
        return apps != null ? apps.get(id) : null;
    }

    private String convertUrl(String url) {

        if (remoteAppInfoProvider == null) {
            return url;
        }

        String baseUrlReplacement;
        String appName = getAppName(url);

        RemoteAppInfo appInfo = remoteAppInfoProvider.getAppInfo(appName);
        if (appInfo == null) {
            appInfo = new RemoteAppInfo();
        }

        RecordsProperties.App app = getAppProps(appName);

        String baseUrl = app != null ? app.getRecBaseUrl() : null;
        String userBaseUrl = app != null ? app.getRecUserBaseUrl() : null;

        if (RemoteRecordsUtils.isSystemContext()) {
            if (baseUrl != null) {
                baseUrlReplacement = baseUrl;
            } else {
                baseUrlReplacement = appInfo.getRecordsBaseUrl();
            }
        } else {
            if (userBaseUrl != null) {
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
        int firstSlashIndex = url.indexOf("/");
        int nextSlashIndex = url.indexOf("/", firstSlashIndex + 1);
        if (firstSlashIndex != -1 && nextSlashIndex != -1) {
            return url.substring(firstSlashIndex + 1, nextSlashIndex);
        }
        return "";
    }
}
