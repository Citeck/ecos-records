package ru.citeck.ecos.records2.rest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;

@Data
@SuppressFBWarnings(value = { "EI_EXPOSE_REP", "EI_EXPOSE_REP2" })
public class RestRequestEntity {

    private HttpHeaders headers = new HttpHeaders();
    private byte[] body;
}
