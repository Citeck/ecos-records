package ru.citeck.ecos.records2.meta.attproc;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;

import java.util.List;

public interface AttProcessor {

    @NotNull
    Object process(@NotNull DataValue value, @NotNull List<DataValue> arguments);

    String getType();
}
