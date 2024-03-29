package ru.citeck.ecos.records3.record.atts.value;

import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.MLText;

import java.util.List;

public interface AttEdge {

    default boolean isProtected() throws Exception {
        return false;
    }

    default boolean isMandatory() throws Exception {
        return false;
    }

    default boolean isMultiple() throws Exception {
        return false;
    }

    default boolean isAssociation() throws Exception {
        return false;
    }

    default boolean isSearchable() throws Exception {
        return true;
    }

    default boolean isUnreadable() throws Exception {
        return false;
    }

    @Nullable
    default List<?> getOptions() throws Exception {
        return null;
    }

    @Nullable
    default List<?> getDistinct() throws Exception {
        return null;
    }

    @Nullable
    default List<?> getCreateVariants() throws Exception {
        return null;
    }

    @Nullable
    default Class<?> getJavaClass() throws Exception {
        return null;
    }

    @Nullable
    default String getEditorKey() throws Exception {
        return null;
    }

    @Nullable
    default String getType() throws Exception {
        return null;
    }

    @Nullable
    default MLText getTitle() throws Exception {
        return null;
    }

    @Nullable
    default MLText getDescription() throws Exception {
        return null;
    }

    @Nullable
    default String getName() throws Exception {
        return null;
    }

    @Nullable
    default Object getValue() throws Exception {
        return null;
    }
}
