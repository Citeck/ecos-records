package ru.citeck.ecos.records2.meta.schema;

import lombok.Data;
import ru.citeck.ecos.records2.meta.util.AttStrUtils;

import java.util.Collections;
import java.util.List;

@Data
public class SchemaAtt {

    private final String alias;
    private final String name;
    private final boolean multiple;
    private final List<SchemaAtt> inner;

    public SchemaAtt(String name) {
        this("", name);
    }

    public SchemaAtt(String alias, String name) {
        this(alias, name, false);
    }

    public SchemaAtt(String alias, String name, boolean multiple) {
        this(alias, name, multiple, Collections.emptyList());
    }

    public SchemaAtt(String alias,
                     String name,
                     boolean multiple,
                     List<SchemaAtt> inner) {

        this.alias = AttStrUtils.removeQuotes(alias);
        this.name = AttStrUtils.removeQuotes(name);
        this.multiple = multiple;
        this.inner = inner;
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
}
