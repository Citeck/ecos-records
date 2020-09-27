package ru.citeck.ecos.records3.source.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records3.RecordRef;

import java.util.Collection;
import java.util.Map;

public interface AttValueCtx {

    @NotNull
    RecordRef getRef() throws Exception;

    @NotNull
    String getLocalId() throws Exception;

    @NotNull
    DataValue getAtt(@NotNull String attribute) throws Exception;

    @Nullable
    <T> T getAtts(@NotNull Class<T> type) throws Exception;

    @NotNull
    ObjectData getAtts(@NotNull Map<String, String> attributes) throws Exception;

    @NotNull
    ObjectData getAtts(@NotNull Collection<String> attributes) throws Exception;
}
