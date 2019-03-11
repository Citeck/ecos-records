package ru.citeck.ecos.records2.graphql.meta.value;

import java.util.List;

public interface MetaEdge {

    default boolean isProtected() {
        return false;
    }

    default boolean isMultiple() {
        return true;
    }

    default List<?> getOptions() {
        return null;
    }

    default List<?> getDistinct() {
        return null;
    }

    default Class<?> getJavaClass() {
        return Object.class;
    }

    default String getTitle() {
        return getName();
    }

    default String getDescription() {
        return "";
    }

    String getName();

    Object getValue() throws Exception;
}
