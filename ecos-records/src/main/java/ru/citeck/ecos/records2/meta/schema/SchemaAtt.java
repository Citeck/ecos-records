package ru.citeck.ecos.records2.meta.schema;

import lombok.Data;
import ru.citeck.ecos.records2.meta.attproc.AttProcessorDef;

import java.util.Collections;
import java.util.List;

@Data
public class SchemaAtt {

    private final String alias;
    private final String name;
    private final boolean multiple;
    private final List<SchemaAtt> inner;
    private final List<AttProcessorDef> processors;

    public SchemaAtt(String name) {
        this("", name);
    }

    public SchemaAtt(String alias, String name) {
        this(alias, name, false);
    }

    public SchemaAtt(String alias, String name, boolean multiple) {
        this(alias, name, multiple, Collections.emptyList());
    }

    public SchemaAtt(String alias, String name, boolean multiple, List<SchemaAtt> inner) {
        this(alias, name, multiple, inner, Collections.emptyList());
    }
    public SchemaAtt(String alias,
                     String name,
                     boolean multiple,
                     List<SchemaAtt> inner,
                     List<AttProcessorDef> processors) {

        this.alias = removeQuotes(alias);
        this.name = removeQuotes(name);
        this.multiple = multiple;
        this.inner = inner;
        this.processors = processors;
    }

    public boolean isScalar() {
        return name.charAt(0) == '.';
    }

    public String getAliasForValue() {
        if (alias.isEmpty()) {
            return name;
        }
        return alias;
    }

    private String removeQuotes(String att) {
        if (att.length() < 2) {
            return att;
        }
        char firstChar = att.charAt(0);
        if (firstChar == att.charAt(att.length() - 1)) {
            if (firstChar == '"' || firstChar == '\'') {
                return att.substring(1, att.length() - 1);
            }
        }
        return att;
    }
}
