package ru.citeck.ecos.records3.graphql.meta.value;

import java.util.List;

public interface MetaEdge {

    default boolean isProtected() throws Exception {
        return false;
    }

    default boolean isMultiple() throws Exception {
        return true;
    }

    default boolean isAssociation() throws Exception {
        return false;
    }

    default boolean isSearchable() throws Exception {
        return true;
    }

    /**
     * Can client read value of this attribute or not.
     */
    default boolean canBeRead() throws Exception {
        return true;
    }

    default List<?> getOptions() throws Exception {
        return null;
    }

    default List<?> getDistinct() throws Exception {
        return null;
    }

    default List<CreateVariant> getCreateVariants() throws Exception {
        return null;
    }

    default Class<?> getJavaClass() throws Exception {
        return Object.class;
    }

    default String getEditorKey() throws Exception {
        return null;
    }

    /**
     * Type of attribute.
     */
    default String getType() throws Exception {
        return null;
    }

    default String getTitle() throws Exception {
        return getName();
    }

    default String getDescription() throws Exception {
        return "";
    }

    String getName() throws Exception ;

    Object getValue() throws Exception;
}
