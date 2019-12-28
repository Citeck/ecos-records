package ru.citeck.ecos.records2.source.common;

import ru.citeck.ecos.records2.graphql.meta.value.MetaField;

public interface AttributesMixin<T> {

    boolean hasAttribute(String attribute);

    Object getAttribute(String attribute, T meta, MetaField field);

    Class<T> getRequiredMetaType();
}
