package ru.citeck.ecos.records2.meta.attproc;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.RecordMeta;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface AttProcessor {

    @NotNull
    Object process(@NotNull RecordMeta meta, @NotNull DataValue value, @NotNull List<DataValue> arguments);

    String getType();

    default Map<String, String> getAttributesToLoad(@NotNull List<DataValue> arguments) {
        return Collections.emptyMap();
    }
}
