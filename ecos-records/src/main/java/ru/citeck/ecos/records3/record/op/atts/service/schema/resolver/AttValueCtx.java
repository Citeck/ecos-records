package ru.citeck.ecos.records3.record.op.atts.service.schema.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;

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
    ObjectData getAtts(@NotNull Map<String, ?> attributes) throws Exception;
}
