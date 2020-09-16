package ru.citeck.ecos.records2.meta.schema;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import ru.citeck.ecos.records2.meta.attproc.AttProcessorDef;

import java.util.Collections;
import java.util.List;

@Data
@RequiredArgsConstructor
public class SchemaAtt {

    private final String alias;
    private final String name;
    private final boolean multiple;
    private final List<SchemaAtt> inner;
    private final List<AttProcessorDef> processors;

    public SchemaAtt(String name) {
        this.alias = name;
        this.name = name;
        this.multiple = false;
        this.inner = Collections.emptyList();
        this.processors = Collections.emptyList();
    }

    public SchemaAtt(String alias, String name) {
        this.alias = alias;
        this.name = name;
        this.multiple = false;
        this.inner = Collections.emptyList();
        this.processors = Collections.emptyList();
    }

    public SchemaAtt(String alias, String name, boolean multiple) {
        this.alias = alias;
        this.name = name;
        this.multiple = multiple;
        this.inner = Collections.emptyList();
        this.processors = Collections.emptyList();
    }

    public SchemaAtt(String alias, String name, boolean multiple, List<SchemaAtt> inner) {
        this.alias = alias;
        this.name = name;
        this.multiple = multiple;
        this.inner = inner;
        this.processors = Collections.emptyList();
    }
}
