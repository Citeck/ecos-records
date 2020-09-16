package ru.citeck.ecos.records2.meta.schema;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class AttsSchema {

    private final Map<String, String> attributes;
    private final List<SchemaAtt> schema;

    public AttsSchema() {
        attributes = Collections.emptyMap();
        schema = Collections.emptyList();
    }
}
