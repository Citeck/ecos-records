package ru.citeck.ecos.records2.graphql.meta.value;

import java.util.List;
import java.util.Map;

/**
 * @deprecated -> you should not use this
 */
@Deprecated
public interface MetaField {

    String getInnerSchema();

    String getAlias();

    String getName();

    String getAttributeSchema(String field);

    List<String> getInnerAttributes();

    Map<String, String> getInnerAttributesMap();

    Map<String, String> getInnerAttributesMap(boolean withAliases);
}
