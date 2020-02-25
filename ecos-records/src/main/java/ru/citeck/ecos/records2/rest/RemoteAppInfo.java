package ru.citeck.ecos.records2.rest;

import lombok.Data;

@Data
public class RemoteAppInfo {

    private String recordsBaseUrl;
    private String recordsUserBaseUrl;

    private String host;
    private String ip;
    private int port;
}
