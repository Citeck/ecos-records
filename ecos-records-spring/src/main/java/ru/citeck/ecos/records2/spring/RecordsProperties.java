package ru.citeck.ecos.records2.spring;

import lombok.Data;

@Data
public class RecordsProperties {

    private RestProps rest;
    private AlfProps alfresco;

    @Data
    public static class RestProps {
        private Boolean secure;
    }

    @Data
    public static class AlfProps {
        private Authentication auth;
    }

    @Data
    public static class Authentication {
        private String username;
        private String password;
    }
}
