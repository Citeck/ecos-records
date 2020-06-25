package ru.citeck.ecos.records2.source.dao.local.meta;

import ru.citeck.ecos.records2.graphql.meta.value.MetaField;

import java.util.List;

public interface MetaAttributesSupplier {

    List<String> getAttributesList();

    Object getAttribute(String attribute, MetaField field) throws Exception;
}
