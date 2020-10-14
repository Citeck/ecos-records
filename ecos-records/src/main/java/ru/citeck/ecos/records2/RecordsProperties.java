package ru.citeck.ecos.records2;

import lombok.Data;

import java.util.Map;

@Data
public class RecordsProperties {

    private String appName = "";
    private String appInstanceId = "";
    private RestProps rest;
    private Map<String, App> apps;
    private Map<String, String> sourceIdMapping;

    /**
     * Disable remote records for testing.
     */
    private boolean forceLocalMode;

    /**
     * Used by gateway.
     */
    private boolean gatewayMode;
    private String defaultApp;

    @Data
    public static class App {
        private String recBaseUrl;
        private String recUserBaseUrl;
        private Authentication auth;
    }

    @Data
    public static class RestProps {
        private Boolean secure;
    }

    @Data
    public static class Authentication {
        private String username;
        private String password;
    }
}
