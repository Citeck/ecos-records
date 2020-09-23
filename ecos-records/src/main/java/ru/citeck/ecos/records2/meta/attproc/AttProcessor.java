package ru.citeck.ecos.records2.meta.attproc;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface AttProcessor {

    @NotNull
    Object process(@NotNull ObjectData attributes, @NotNull DataValue value, @NotNull List<DataValue> arguments);

    @NotNull
    String getType();

    @NotNull
    default Map<String, String> getAttributesToLoad(@NotNull List<DataValue> arguments) {
        return Collections.emptyMap();
    }
}
