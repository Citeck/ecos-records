package ru.citeck.ecos.records2.meta.schema;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class AttsSchema {

    private final Map<String, String> sourceAtts;
    private final List<SchemaRootAtt> attributes;

    public AttsSchema() {
        sourceAtts = Collections.emptyMap();
        attributes = Collections.emptyList();
    }
}
