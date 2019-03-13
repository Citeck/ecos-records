package ru.citeck.ecos.records2.graphql.meta.value;

import java.util.List;

public interface MetaField {

    String getInnerSchema();

    String getAlias();

    String getName();

    String getAttributeSchema(String field);

    List<String> getInnerAttributes();
}
