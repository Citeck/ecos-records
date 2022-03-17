package ru.citeck.ecos.records3.record.atts.value.factory.bean;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.record.atts.value.AttEdge;

/**
 * Experimental interface. May be changed in future
 */
public interface BeanAttValue {

    @Nullable
    default Object getId() throws Exception {
        return null;
    }

    @Nullable
    default Object getAs(@NotNull String name) throws Exception {
        return null;
    }

    @Nullable
    default Object getDisplayName() throws Exception {
        return toString();
    }

    @Nullable
    default Object getEcosType() throws Exception {
        return null;
    }

    @Nullable
    default Object getAtt(@NotNull String name) throws Exception {
        return null;
    }

    @Nullable
    default AttEdge getEdge(@NotNull String name) throws Exception {
        return null;
    }

    default boolean has(@NotNull String name) throws Exception {
        return false;
    }
}
