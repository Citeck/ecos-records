package ru.citeck.ecos.records2.meta;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.field.EmptyMetaField;

import java.util.Collections;
import java.util.Map;

@Data
@AllArgsConstructor
public class AttributesSchema {

    private String gqlSchema;
    private Map<String, AttSchemaInfo> attsInfo;
    private MetaField metaField;
    private Map<String, String> attributes;

    public AttributesSchema() {
        this.gqlSchema = "";
        this.attsInfo = Collections.emptyMap();
        this.metaField = EmptyMetaField.INSTANCE;
        this.attributes = Collections.emptyMap();
    }
}
