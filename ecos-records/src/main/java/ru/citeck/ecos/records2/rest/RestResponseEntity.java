package ru.citeck.ecos.records2.rest;

import lombok.Data;

@Data
public class RestResponseEntity {

    private HttpHeaders headers = new HttpHeaders();
    private byte[] body;
    private int status;
}
