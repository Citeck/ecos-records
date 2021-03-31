package ru.citeck.ecos.records2.graphql.meta.value;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @deprecated use AttEdge instead
 */
@Deprecated
public interface MetaEdge {

    default boolean isProtected() {
        return false;
    }

    default boolean isMultiple() {
        return true;
    }

    default boolean isAssociation() {
        return false;
    }

    default boolean isSearchable() {
        return true;
    }

    /**
     * Can client read value of this attribute or not.
     */
    default boolean isUnreadable() {
        return false;
    }

    default List<?> getOptions() {
        return null;
    }

    default List<?> getDistinct() {
        return null;
    }

    default List<CreateVariant> getCreateVariants() {
        return null;
    }

    default Class<?> getJavaClass() {
        return Object.class;
    }

    default String getEditorKey() {
        return null;
    }

    /**
     * Type of attribute.
     */
    default String getType() {
        return null;
    }

    default String getTitle() {
        return getName();
    }

    default String getDescription() {
        return "";
    }

    String getName();

    Object getValue(@NotNull MetaField field) throws Exception;
}
